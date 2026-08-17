package com.lawfirm.knowledge;

import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.knowledge.dto.ArticleRequest;
import com.lawfirm.knowledge.dto.ArticleView;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final UserRepository userRepository;

    /** 列表：普通用户只看已发布；作者本人可看自己的草稿；管理员全部可见 */
    public PageResult<ArticleView> page(String keyword, KnowledgeCategory category, int page, int size) {
        Long me = CurrentUser.id();
        boolean admin = CurrentUser.isAdmin();
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<KnowledgeArticle> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), like),
                        cb.like(root.get("tags"), like)
                ));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (!admin) {
                predicates.add(cb.or(
                        cb.isTrue(root.get("published")),
                        cb.equal(root.get("authorId"), me)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<KnowledgeArticle> result = knowledgeRepository.findAll(spec, pageable);
        Map<Long, String> names = userNameMap(result.getContent().stream()
                .map(KnowledgeArticle::getAuthorId).distinct().toList());
        return PageResult.of(result, a -> toView(a, names));
    }

    @Transactional
    public ArticleView detail(Long id) {
        KnowledgeArticle article = knowledgeRepository.findById(id)
                .orElseThrow(() -> new BizException("文章不存在"));
        boolean visible = Boolean.TRUE.equals(article.getPublished())
                || article.getAuthorId().equals(CurrentUser.id())
                || CurrentUser.isAdmin();
        if (!visible) {
            throw new BizException(403, "该文章未发布");
        }
        article.setViewCount(article.getViewCount() + 1);
        article = knowledgeRepository.save(article);
        return toView(article, userNameMap(List.of(article.getAuthorId())));
    }

    @Transactional
    public ArticleView create(ArticleRequest request) {
        KnowledgeArticle article = new KnowledgeArticle();
        article.setTitle(request.title());
        article.setCategory(request.category());
        article.setContent(request.content());
        article.setTags(request.tags());
        article.setPublished(request.published() == null || request.published());
        article.setAuthorId(CurrentUser.id());
        return toView(knowledgeRepository.save(article), Map.of());
    }

    @Transactional
    public ArticleView update(Long id, ArticleRequest request) {
        KnowledgeArticle article = getById(id);
        if (!article.getAuthorId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只能修改自己发布的文章");
        }
        article.setTitle(request.title());
        article.setCategory(request.category());
        article.setContent(request.content());
        article.setTags(request.tags());
        article.setPublished(request.published() == null || request.published());
        return toView(knowledgeRepository.save(article), Map.of());
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeArticle article = getById(id);
        if (!article.getAuthorId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只能删除自己发布的文章");
        }
        knowledgeRepository.delete(article);
    }

    private KnowledgeArticle getById(Long id) {
        return knowledgeRepository.findById(id).orElseThrow(() -> new BizException("文章不存在"));
    }

    private ArticleView toView(KnowledgeArticle a, Map<Long, String> names) {
        return new ArticleView(a.getId(), a.getTitle(), a.getCategory(), a.getContent(), a.getAuthorId(),
                names.getOrDefault(a.getAuthorId(), ""), a.getTags(), a.getViewCount(),
                a.getPublished(), a.getCreatedAt());
    }

    private Map<Long, String> userNameMap(List<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getRealName));
    }
}
