package com.dc.smarteam.modules.client.service;

import com.dc.smarteam.common.service.LongCrudService;
import com.dc.smarteam.modules.client.dao.ClientConfigLogDao;
import com.dc.smarteam.modules.client.entity.ClientConfigLog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by huangzbb on 2016/8/3.
 */
@Service
@Transactional
public class ClientConfigLogService extends LongCrudService<ClientConfigLogDao, ClientConfigLog> {

    @Transactional
    public void insert(ClientConfigLog clientConfigLog){
        dao.insert(clientConfigLog);
    }

}
