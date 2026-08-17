package com.lawfirm.calendar;

import com.lawfirm.calendar.dto.EventRequest;
import com.lawfirm.calendar.dto.EventView;
import com.lawfirm.common.BizException;
import com.lawfirm.security.CurrentUser;
import com.lawfirm.user.User;
import com.lawfirm.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarEventRepository eventRepository;
    private final UserRepository userRepository;

    /** 时间段内的事件 */
    public List<EventView> events(LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByStartTimeBetweenOrderByStartTimeAsc(start, end).stream()
                .map(this::toView).toList();
    }

    /** 我的事件：我创建的或我参与的 */
    public List<EventView> myEvents(LocalDateTime start, LocalDateTime end) {
        Long me = CurrentUser.id();
        return eventRepository.findByStartTimeBetweenOrderByStartTimeAsc(start, end).stream()
                .filter(e -> e.getCreatorId().equals(me)
                        || (e.getParticipantIds() != null && e.getParticipantIds().contains(me)))
                .map(this::toView).toList();
    }

    @Transactional
    public EventView create(EventRequest request) {
        CalendarEvent event = new CalendarEvent();
        apply(event, request);
        event.setCreatorId(CurrentUser.id());
        return toView(eventRepository.save(event));
    }

    @Transactional
    public EventView update(Long id, EventRequest request) {
        CalendarEvent event = getById(id);
        if (!event.getCreatorId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只能修改自己创建的日程");
        }
        apply(event, request);
        return toView(eventRepository.save(event));
    }

    @Transactional
    public void delete(Long id) {
        CalendarEvent event = getById(id);
        if (!event.getCreatorId().equals(CurrentUser.id()) && !CurrentUser.isAdmin()) {
            throw new BizException(403, "只能删除自己创建的日程");
        }
        eventRepository.delete(event);
    }

    private void apply(CalendarEvent event, EventRequest request) {
        event.setTitle(request.title());
        event.setType(request.type());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setLocation(request.location());
        event.setDescription(request.description());
        event.setCaseId(request.caseId());
        event.setParticipantIds(request.participantIds() == null
                ? new ArrayList<>() : new ArrayList<>(request.participantIds()));
    }

    private CalendarEvent getById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new BizException("日程不存在"));
    }

    private EventView toView(CalendarEvent e) {
        String creatorName = userRepository.findById(e.getCreatorId()).map(User::getRealName).orElse("");
        Set<Long> ids = e.getParticipantIds() == null ? Set.of() : Set.copyOf(e.getParticipantIds());
        Map<Long, String> names = ids.isEmpty() ? Map.of()
                : userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, User::getRealName));
        List<String> participantNames = ids.stream().map(id -> names.getOrDefault(id, "")).toList();
        return new EventView(e.getId(), e.getTitle(), e.getType(), e.getStartTime(), e.getEndTime(),
                e.getLocation(), e.getDescription(), e.getCaseId(), e.getCreatorId(), creatorName,
                new ArrayList<>(ids), participantNames, e.getCreatedAt());
    }
}
