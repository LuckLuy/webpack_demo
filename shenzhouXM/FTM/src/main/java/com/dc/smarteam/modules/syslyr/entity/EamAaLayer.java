/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.dc.smarteam.modules.syslyr.entity;

import com.dc.smarteam.common.persistence.DataEntity;
import org.hibernate.validator.constraints.Length;

/**
 * 系统架构层次管理Entity
 *
 * @author yangqjb
 * @version 2015-12-24
 */
public class EamAaLayer extends DataEntity<EamAaLayer> {

    private static final long serialVersionUID = 1L;
    private String name;        // 架构层次名称
    private String chineseName;        // 架构层次简称

    public EamAaLayer() {
        super();
    }

    public EamAaLayer(String id) {
        super(id);
    }

    @Length(min = 0, max = 250, message = "架构层次名称长度必须介于 0 和 250 之间")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Length(min = 0, max = 250, message = "架构层次简称长度必须介于 0 和 250 之间")
    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }

}