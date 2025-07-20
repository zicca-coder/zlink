package com.zicca.zlink.framework.service;

import com.zicca.zlink.framework.idempotent.IdempotentStatus;

public interface IdempotentService {

    IdempotentStatus tryInsertKey(String key);


    void markConsumed(String key);


    void deleteKey(String key);

}
