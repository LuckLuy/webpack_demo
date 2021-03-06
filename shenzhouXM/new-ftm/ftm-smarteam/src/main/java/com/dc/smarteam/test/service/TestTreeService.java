/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.dc.smarteam.test.service;

import com.dc.smarteam.common.service.TreeService;
import com.dc.smarteam.common.utils.StringUtils;
import com.dc.smarteam.test.dao.TestTreeDao;
import com.dc.smarteam.test.entity.TestTree;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 树结构生成Service
 *
 * @author ThinkGem
 * @version 2015-04-06
 */
@Service
@Transactional(readOnly = true)
public class TestTreeService extends TreeService<TestTreeDao, TestTree> {

    public TestTree get(String id) {
        return super.get(id);
    }

    public List<TestTree> findList(TestTree testTree) {
        if (StringUtils.isNotBlank(testTree.getParentIds())) {
            testTree.setParentIds("," + testTree.getParentIds() + ",");
        }
        return super.findList(testTree);
    }

    @Transactional(readOnly = false)
    public void save(TestTree testTree) {
        super.save(testTree);
    }

    @Transactional(readOnly = false)
    public void delete(TestTree testTree) {
        super.delete(testTree);
    }

}