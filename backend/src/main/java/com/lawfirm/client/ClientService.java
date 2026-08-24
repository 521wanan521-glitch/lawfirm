package com.lawfirm.client;

import com.lawfirm.client.dto.ClientRequest;
import com.lawfirm.client.dto.ClientView;
import com.lawfirm.client.dto.ContactRequest;
import com.lawfirm.client.dto.ContactView;
import com.lawfirm.client.dto.InteractionRequest;
import com.lawfirm.client.dto.InteractionView;
import com.lawfirm.common.BizException;
import com.lawfirm.common.PageResult;
import com.lawfirm.cases.CaseRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ContactRepository contactRepository;
    private final InteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;

    public PageResult<ClientView> page(String keyword, Client.Level level, Long ownerId, Boolean consultant, int page, int size) {
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Client> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("phone"), like),
                        cb.like(root.get("idNumber"), like)
                ));
            }
            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            if (ownerId != null) {
                predicates.add(cb.equal(root.get("ownerId"), ownerId));
            }
            if (consultant != null) {
                predicates.add(cb.equal(root.get("consultant"), consultant));
            }
            if (!CurrentUser.isManager()) {
                predicates.add(cb.equal(root.get("ownerId"), CurrentUser.id()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<Client> result = clientRepository.findAll(spec, pageable);
        Map<Long, String> userNames = userNameMap(result.getContent().stream()
                .map(Client::getOwnerId).filter(java.util.Objects::nonNull).distinct().toList());
        return PageResult.of(result, c -> toView(c, userNames));
    }

    @Transactional(readOnly = true)
    public ClientView detail(Long id) {
        Client client = getById(id);
        checkClientAccess(client);
        Map<Long, String> userNames = userNameMap(client.getOwnerId() == null ? List.of() : List.of(client.getOwnerId()));
        return toView(client, userNames);
    }

    public PageResult<InteractionView> interactions(Long clientId, int page, int size) {
        checkClientAccess(getById(clientId));
        PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Interaction> result = interactionRepository.findByClientIdOrderByCreatedAtDesc(clientId, pageable);
        Map<Long, String> userNames = userNameMap(result.getContent().stream()
                .map(Interaction::getUserId).distinct().toList());
        return PageResult.of(result, i -> new InteractionView(
                i.getId(), i.getType(), i.getContent(), i.getNextFollowDate(),
                i.getUserId(), userNames.get(i.getUserId()), i.getCreatedAt()));
    }

    @Transactional
    public ClientView create(ClientRequest request) {
        Client client = new Client();
        apply(client, request);
        return toView(clientRepository.save(client), Map.of());
    }

    @Transactional
    public ClientView update(Long id, ClientRequest request) {
        Client client = getById(id);
        checkClientAccess(client);
        apply(client, request);
        return toView(clientRepository.save(client), Map.of());
    }

    @Transactional
    public void delete(Long id) {
        Client client = getById(id);
        checkClientAccess(client);
        contactRepository.deleteByClientId(id);
        interactionRepository.deleteByClientId(id);
        clientRepository.deleteById(id);
    }

    // ---------- 联系人 ----------

    public List<ContactView> contacts(Long clientId) {
        checkClientAccess(getById(clientId));
        return contactRepository.findByClientIdOrderByCreatedAtAsc(clientId).stream()
                .map(ContactView::from).toList();
    }

    @Transactional
    public ContactView addContact(Long clientId, ContactRequest request) {
        checkClientAccess(getById(clientId));
        Contact contact = new Contact();
        contact.setClientId(clientId);
        apply(contact, request);
        return ContactView.from(contactRepository.save(contact));
    }

    @Transactional
    public ContactView updateContact(Long clientId, Long contactId, ContactRequest request) {
        checkClientAccess(getById(clientId));
        Contact contact = getContact(clientId, contactId);
        apply(contact, request);
        return ContactView.from(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(Long clientId, Long contactId) {
        checkClientAccess(getById(clientId));
        Contact contact = getContact(clientId, contactId);
        contactRepository.delete(contact);
    }

    // ---------- 跟进记录 ----------

    @Transactional
    public InteractionView addInteraction(Long clientId, InteractionRequest request) {
        checkClientAccess(getById(clientId));
        Interaction interaction = new Interaction();
        interaction.setClientId(clientId);
        interaction.setUserId(CurrentUser.id());
        interaction.setType(request.type());
        interaction.setContent(request.content());
        interaction.setNextFollowDate(request.nextFollowDate());
        interaction = interactionRepository.save(interaction);
        return new InteractionView(interaction.getId(), interaction.getType(), interaction.getContent(),
                interaction.getNextFollowDate(), interaction.getUserId(), CurrentUser.user().getRealName(),
                interaction.getCreatedAt());
    }

    // ---------- 私有方法 ----------

    private void apply(Client client, ClientRequest request) {
        client.setName(request.name());
        client.setType(request.type());
        client.setIdNumber(request.idNumber());
        client.setIndustry(request.industry());
        client.setAddress(request.address());
        client.setPhone(request.phone());
        client.setEmail(request.email());
        client.setLevel(request.level() == null ? Client.Level.C : request.level());
        client.setSource(request.source());
        client.setOwnerId(request.ownerId());
        client.setRemark(request.remark());
        client.setConsultant(Boolean.TRUE.equals(request.consultant()));
    }

    private void apply(Contact contact, ContactRequest request) {
        contact.setName(request.name());
        contact.setPhone(request.phone());
        contact.setEmail(request.email());
        contact.setPosition(request.position());
        contact.setPrimaryContact(Boolean.TRUE.equals(request.primaryContact()));
    }

    private ClientView toView(Client c, Map<Long, String> userNames) {
        long contactCount = contactRepository.countByClientId(c.getId());
        long caseCount = caseRepository.countByClientId(c.getId());
        return new ClientView(c.getId(), c.getName(), c.getType(), c.getIdNumber(), c.getIndustry(),
                c.getAddress(), c.getPhone(), c.getEmail(), c.getLevel(), c.getSource(), c.getOwnerId(),
                userNames.get(c.getOwnerId()), c.getRemark(), c.getConsultant(), contactCount, caseCount, c.getCreatedAt());
    }

    private Client getById(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new BizException("客户不存在"));
    }

    /** 客户数据权限：仅负责人或管理员（合伙人）可见 */
    private void checkClientAccess(Client client) {
        if (CurrentUser.isManager()) {
            return;
        }
        if (!CurrentUser.id().equals(client.getOwnerId())) {
            throw new BizException(403, "无权访问该客户");
        }
    }

    private Contact getContact(Long clientId, Long contactId) {
        Contact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new BizException("联系人不存在"));
        if (!contact.getClientId().equals(clientId)) {
            throw new BizException("联系人不属于该客户");
        }
        return contact;
    }

    private Map<Long, String> userNameMap(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));
    }
}
