package com.lawfirm.calendar;

import com.lawfirm.calendar.dto.EventRequest;
import com.lawfirm.calendar.dto.EventView;
import com.lawfirm.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/calendar/events")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ApiResponse<List<EventView>> events(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.ok(calendarService.events(start, end));
    }

    @GetMapping("/mine")
    public ApiResponse<List<EventView>> myEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.ok(calendarService.myEvents(start, end));
    }

    @PostMapping
    public ApiResponse<EventView> create(@Valid @RequestBody EventRequest request) {
        return ApiResponse.ok(calendarService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<EventView> update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ApiResponse.ok(calendarService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        calendarService.delete(id);
        return ApiResponse.ok();
    }
}
