package com.hac.cabinet.service;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExternalCreditHistoryServiceMock {
  public boolean approveCompanyCreditHistory(UUID companyId) {
    // здесь вызов некоторого внешнего сервиса
    // для подтверждения кредитной истории, введенной юзером
    // результаты сохраняются в базу и обновляются раз в некоторый промежуток времени
    return true;
  }
}

