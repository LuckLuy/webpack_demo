/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.dc.smarteam.test.service;

import com.dc.smarteam.common.persistence.Page;
import com.dc.smarteam.common.service.CrudService;
import com.dc.smarteam.test.dao.TestDataDao;
import com.dc.smarteam.test.entity.TestData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 单表生成Service
 *
 * @author ThinkGem
 * @version 2015-04-06
 */
@Service
@Transactional(readOnly = true)
public class TestDataService extends CrudService<TestDataDao, TestData> {

    public TestData get(String id) {
        return super.get(id);
    }

    public List<TestData> findList(TestData testData) {
        return super.findList(testData);
    }

    public Page<TestData> findPage(Page<TestData> page, TestData testData) {
        return super.findPage(page, testData);
    }

    @Transactional(readOnly = false)
    public void save(TestData testData) {
        super.save(testData);
    }

    @Transactional(readOnly = false)
    public void delete(TestData testData) {
        super.delete(testData);
    }

}