package com.lawfirm.client;

import com.lawfirm.client.dto.ClientRequest;
import com.lawfirm.client.dto.ClientView;
import com.lawfirm.client.dto.ContactRequest;
import com.lawfirm.client.dto.ContactView;
import com.lawfirm.client.dto.InteractionRequest;
import com.lawfirm.client.dto.InteractionView;
import com.lawfirm.common.ApiResponse;
import com.lawfirm.common.PageResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ApiResponse<PageResult<ClientView>> page(@RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Client.Level level,
                                                    @RequestParam(required = false) Long ownerId,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(clientService.page(keyword, level, ownerId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ClientView> detail(@PathVariable Long id) {
        return ApiResponse.ok(clientService.detail(id));
    }

    @PostMapping
    public ApiResponse<ClientView> create(@Valid @RequestBody ClientRequest request) {
        return ApiResponse.ok(clientService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ClientView> update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return ApiResponse.ok(clientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ApiResponse.ok();
    }

    // ---------- 联系人 ----------

    @GetMapping("/{id}/contacts")
    public ApiResponse<List<ContactView>> contacts(@PathVariable Long id) {
        return ApiResponse.ok(clientService.contacts(id));
    }

    @PostMapping("/{id}/contacts")
    public ApiResponse<ContactView> addContact(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ApiResponse.ok(clientService.addContact(id, request));
    }

    @PutMapping("/{id}/contacts/{contactId}")
    public ApiResponse<ContactView> updateContact(@PathVariable Long id, @PathVariable Long contactId,
                                                  @Valid @RequestBody ContactRequest request) {
        return ApiResponse.ok(clientService.updateContact(id, contactId, request));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    public ApiResponse<Void> deleteContact(@PathVariable Long id, @PathVariable Long contactId) {
        clientService.deleteContact(id, contactId);
        return ApiResponse.ok();
    }

    // ---------- 跟进记录 ----------

    @GetMapping("/{id}/interactions")
    public ApiResponse<PageResult<InteractionView>> interactions(@PathVariable Long id,
                                                                 @RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(clientService.interactions(id, page, size));
    }

    @PostMapping("/{id}/interactions")
    public ApiResponse<InteractionView> addInteraction(@PathVariable Long id, @Valid @RequestBody InteractionRequest request) {
        return ApiResponse.ok(clientService.addInteraction(id, request));
    }
}
