package com.dc.smarteam.modules.file.web;

import com.dc.smarteam.common.json.ResultDtoTool;
import com.dc.smarteam.common.persistence.Page;
import com.dc.smarteam.common.utils.StringUtils;
import com.dc.smarteam.helper.CurrNameNodeHelper;
import com.dc.smarteam.modules.file.entity.BizFile;
import com.dc.smarteam.modules.file.service.BizFileService;
import com.dc.smarteam.modules.servicenode.entity.FtServiceNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  文件列表
 * lyy
 *  2020.10.22
 */
@RestController
@RequestMapping(value = "${adminPath}/file/bizFile")
public class BizFileController {

    @Autowired
    private BizFileService bizFileService;

    @ModelAttribute
    public BizFile get(@RequestParam(required = false) String id) {
        if (StringUtils.isNotBlank(id)) {
            return bizFileService.get(id);
        } else {
            return new BizFile();
        }
    }


    /**
     *   文件列表
     * @param bizFile
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(value = {"list", ""})
    public Object list(BizFile bizFile, HttpServletRequest request, HttpServletResponse response) {
        FtServiceNode ftServiceNode = CurrNameNodeHelper.getCurrNameNode(request);
        if (null == ftServiceNode || null == ftServiceNode.getSystemName()) {
           return ResultDtoTool.buildError("请先设置节点组！！！");

        }
        Map<String,Object> resultMap = new HashMap<>();
        List<String> nodeNameList = bizFileService.findNodeNameList();
        resultMap.put("nodeNameList", nodeNameList);

        Page<BizFile> page = bizFileService.findPage(new Page<BizFile>(request, response), bizFile);
        bizFile.setPage(page);
        resultMap.put("page", page);
        return ResultDtoTool.buildSucceed(resultMap);
    }

    @RequestMapping(value = "form" ,produces = "application/json;charset=UTF-8")
    public Object form(BizFile bizFile) {
        Map<String,Object> re =  new HashMap<>();
        re.put("bizFile", bizFile);
        return  ResultDtoTool.buildSucceed(re);
    }

}
