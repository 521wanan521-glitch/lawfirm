package com.lawfirm.client.dto;

import com.lawfirm.client.Contact;

public record ContactView(
        Long id,
        String name,
        String phone,
        String email,
        String position,
        Boolean primaryContact
) {
    public static ContactView from(Contact c) {
        return new ContactView(c.getId(), c.getName(), c.getPhone(), c.getEmail(), c.getPosition(), c.getPrimaryContact());
    }
}
