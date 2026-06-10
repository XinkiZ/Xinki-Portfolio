package com.xinki.portfolio.service.impl;

import com.xinki.portfolio.entity.ContactMessage;
import com.xinki.portfolio.mapper.ContactMessageMapper;
import com.xinki.portfolio.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final ContactMessageMapper contactMessageMapper;

    @Override
    public void submit(ContactMessage message) {
        contactMessageMapper.insert(message);
    }
}
