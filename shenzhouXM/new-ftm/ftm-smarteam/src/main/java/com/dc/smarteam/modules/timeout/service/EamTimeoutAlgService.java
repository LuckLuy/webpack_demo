/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.dc.smarteam.modules.timeout.service;

import com.dc.smarteam.common.persistence.Page;
import com.dc.smarteam.common.service.CrudService;
import com.dc.smarteam.modules.timeout.dao.EamTimeoutAlgDao;
import com.dc.smarteam.modules.timeout.entity.EamTimeoutAlg;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 超时时间管理Service
 *
 * @author kern
 * @version 2015-12-24
 */
@Service
@Transactional(readOnly = true)
public class EamTimeoutAlgService extends CrudService<EamTimeoutAlgDao, EamTimeoutAlg> {

    public EamTimeoutAlg get(String id) {
        return super.get(id);
    }

    public List<EamTimeoutAlg> findList(EamTimeoutAlg eamTimeoutAlg) {
        return super.findList(eamTimeoutAlg);
    }

    public Page<EamTimeoutAlg> findPage(Page<EamTimeoutAlg> page, EamTimeoutAlg eamTimeoutAlg) {
        return super.findPage(page, eamTimeoutAlg);
    }

    @Transactional(readOnly = false)
    public void save(EamTimeoutAlg eamTimeoutAlg) {
        super.save(eamTimeoutAlg);
    }

    @Transactional(readOnly = false)
    public void delete(EamTimeoutAlg eamTimeoutAlg) {
        super.delete(eamTimeoutAlg);
    }

}