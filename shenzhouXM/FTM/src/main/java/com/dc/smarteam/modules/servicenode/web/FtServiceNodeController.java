/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.dc.smarteam.modules.servicenode.web;

import com.dc.smarteam.cfgmodel.CfgModelConverter;
import com.dc.smarteam.cfgmodel.NodesModel;
import com.dc.smarteam.common.adapter.TCPAdapter;
import com.dc.smarteam.common.config.Global;
import com.dc.smarteam.common.json.ResultDto;
import com.dc.smarteam.common.json.ResultDtoTool;
import com.dc.smarteam.common.msggenerator.MessageFactory;
import com.dc.smarteam.common.utils.StringUtils;
import com.dc.smarteam.common.web.BaseController;
import com.dc.smarteam.helper.CurrNameNodeHelper;
import com.dc.smarteam.helper.PageHelper;
import com.dc.smarteam.modules.monitor.ftnodemonitor.entity.FtNodeMonitor;
import com.dc.smarteam.modules.monitor.ftnodemonitor.service.FtNodeMonitorService;
import com.dc.smarteam.modules.nodeparam.entity.FtNodeParam;
import com.dc.smarteam.modules.servicenode.entity.FtServiceNode;
import com.dc.smarteam.modules.sysinfo.entity.FtSysInfo;
import com.dc.smarteam.modules.sysinfo.service.FtSysInfoService;
import com.dc.smarteam.service.NodesService;
import net.sf.json.JSONObject;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 节点管理Controller
 *
 * @author liwang
 * @version 2016-01-11
 */
@Controller
@RequestMapping(value = "${adminPath}/servicenode/ftServiceNode")
public class FtServiceNodeController extends BaseController {

  //20160628 增加系统service
  @Resource
  private FtSysInfoService ftSysInfoService;
  @Resource
  private NodesService nodesService;
  @Resource
  private FtNodeMonitorService ftNodeMonitorService;
  //ADD 20170908
  private static String DATA_NODE = "datanode";

  @RequiresPermissions("servicenode:ftServiceNode:view")
  @RequestMapping(value = {"list", ""})
  public String list(FtServiceNode ftServiceNode, HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {//NOSONAR
    ResultDto<List<NodesModel.Node>> resultDto = nodesService.listAll(null, request);
    List<FtServiceNode> list2 = new ArrayList<>();
    if (ResultDtoTool.isSuccess(resultDto)) {
      List<NodesModel.Node> nodeList = resultDto.getData();
      for (NodesModel.Node node : nodeList) {
        boolean isMatch = true;
        if (isMatch && StringUtils.isNoneEmpty(ftServiceNode.getName())//NOSONAR
            && !StringUtils.containsIgnoreCase(node.getName(), ftServiceNode.getName())) {
          isMatch = false;
        }
        if (isMatch && StringUtils.isNoneEmpty(ftServiceNode.getIpAddress())//NOSONAR
            && !StringUtils.containsIgnoreCase(node.getIp(), ftServiceNode.getIpAddress())) {
          isMatch = false;
        }
        if (!isMatch) continue;
        FtServiceNode ftServiceNode2 = new FtServiceNode();
        CfgModelConverter.convertTo(node, ftServiceNode2);
        if (isMatch && StringUtils.isNoneEmpty(ftServiceNode.getState())//NOSONAR
            && !StringUtils.equalsIgnoreCase(ftServiceNode2.getState(), ftServiceNode.getState())) {
          isMatch = false;
        }
        //add 20170908 查询任务数量
        if (isMatch && null != ftServiceNode2.getType() && ftServiceNode2.getType().equals(DATA_NODE)) {
          int taskCount = 0;
          if (ftServiceNode2.getState().equals("1") || ftServiceNode2.getState().equals("RUNNING")) {
            FtNodeParam ftNodeParam = new FtNodeParam();
            ftNodeParam.setName("task");
            taskCount = queryTaskCount(ftServiceNode2, ftNodeParam, "currResource", "taskCountMap");
          }

          ftServiceNode2.setTaskCount(String.valueOf(taskCount));
        }
        if (!isMatch) continue;
        list2.add(ftServiceNode2);
      }
    } else {
      String data = resultDto.getMessage();
      addMessage(redirectAttributes, data);
      return "redirect:" + Global.getAdminPath() + "/servicenode/ftServiceNode/nodeList";
    }

    PageHelper.getInstance().getPage(FtServiceNode.class, request, response, model, list2);

    return "modules/servicenode/ftServiceNodeList";
  }

  @RequiresPermissions("servicenode:ftServiceNode:baseList")
  @RequestMapping(value = {"baseList"})
  public String baseList(FtServiceNode ftServiceNode, FtSysInfo ftSysInfo, HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
    FtServiceNode ftServiceNode0 = CurrNameNodeHelper.getCurrNameNode(request);
    if (null == ftServiceNode0 || null == ftServiceNode0.getSystemName()) {
      return "redirect:" + Global.getAdminPath() + "/servicenode/ftServiceNode/nodeList";
    }
    String currSysname = ftServiceNode0.getSystemName();
    ResultDto<List<NodesModel.Node>> resultDto = nodesService.listAll(null, request);
    List<FtServiceNode> list2 = new ArrayList<>();
    if (ResultDtoTool.isSuccess(resultDto)) {
      List<NodesModel.Node> nodeList = resultDto.getData();
      for (NodesModel.Node node : nodeList) {
        boolean isMatch = node.getSystem() != null && StringUtils.containsIgnoreCase(node.getSystem().getName(), currSysname);
        if (!isMatch) continue;
        FtServiceNode ftServiceNode2 = new FtServiceNode();
        CfgModelConverter.convertTo(node, ftServiceNode2);
        list2.add(ftServiceNode2);
      }
    } else {
      String data = resultDto.getMessage();
      addMessage(redirectAttributes, data);
      return "redirect:" + Global.getAdminPath() + "/servicenode/ftServiceNode/nodeList";
    }
    PageHelper.getInstance().getPage(FtServiceNode.class, request, response, model, list2);

    return "modules/servicenode/ftServiceNodeBaseList";
  }


  @RequiresPermissions("servicenode:ftServiceNode:view")
  @RequestMapping(value = "form")
  public String form(FtServiceNode ftServiceNode, HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
    if (null != ftServiceNode.getName()) {
      ResultDto<NodesModel.Node> resultDto = nodesService.selByName(ftServiceNode, request);
      if (ResultDtoTool.isSuccess(resultDto)) {
        NodesModel.Node node = resultDto.getData();
        CfgModelConverter.convertTo(node, ftServiceNode);
        ftServiceNode.setAddOrUpdate(1);
        model.addAttribute("ftServiceNode", ftServiceNode);

        List<FtSysInfo> ftSysInfoList = ftSysInfoService.findList(new FtSysInfo());
        List<FtSysInfo> systemNameList = new ArrayList<>();
        for (FtSysInfo fsi : ftSysInfoList) {
          if (fsi.getName().equalsIgnoreCase(ftServiceNode.getSystemName())) {
            systemNameList.add(fsi);
          }
        }
        model.addAttribute("systemNameList", systemNameList);
      }
      model.addAttribute("opt", "update");
    } else {
      List<FtSysInfo> systemNameList = ftSysInfoService.findList(new FtSysInfo());
      model.addAttribute("systemNameList", systemNameList);
      model.addAttribute("opt", "add");
    }
    return "modules/servicenode/ftServiceNodeForm";
  }

  @RequiresPermissions("servicenode:ftServiceNode:edit")
  @RequestMapping(value = "save")
  public String save(String opt, FtServiceNode ftServiceNode, HttpServletRequest request, Model model, RedirectAttributes redirectAttributes) {
    List<FtSysInfo> ftSysInfoList = ftSysInfoService.findList(new FtSysInfo());
    for (FtSysInfo fsi : ftSysInfoList) {
      if (fsi.getName().equalsIgnoreCase(ftServiceNode.getSystemName())) {
        ftServiceNode.setSysNodeModel(fsi.getSysNodeModel());
        ftServiceNode.setSwitchModel(fsi.getSwitchModel());
        ftServiceNode.setStoreModel(fsi.getStoreModel());
        if (!ftServiceNode.getSysNodeModel().contains("ms")) {
          ftServiceNode.setNodeModel(ftServiceNode.getSysNodeModel());
          ftServiceNode.setSwitchModel(null);
        }
      }
    }

    ResultDto<String> resultDto = null;
    if ("add".equals(opt)) {
      ftServiceNode.setState("0");
      if (ftServiceNode.getType().equalsIgnoreCase("namenode") || ftServiceNode.getType().equalsIgnoreCase("lognode")) {
        ftServiceNode.setSysNodeModel(null);
        ftServiceNode.setNodeModel(null);
        ftServiceNode.setStoreModel(null);
        ftServiceNode.setSwitchModel(null);
        ftServiceNode.setSystemName(null);
        if (ftServiceNode.getType().equalsIgnoreCase("lognode")) {
          ftServiceNode.setFtpManagePort(null);
          ftServiceNode.setFtpServPort(null);
          ftServiceNode.setReceivePort(null);
        }
      }
      resultDto = nodesService.add(ftServiceNode, request);
    } else if ("update".equals(opt)) {
      resultDto = nodesService.update(ftServiceNode, request);
    }

    if (ResultDtoTool.isSuccess(resultDto)) {
      addMessage(redirectAttributes, "保存节点成功");
      FtNodeMonitor ftNodeMonitor = new FtNodeMonitor();

      ftNodeMonitor.setNode(ftServiceNode.getName());
      ftNodeMonitor.setPort(ftServiceNode.getCmdPort());
      ftNodeMonitor.setIp(ftServiceNode.getIpAddress());

      ftNodeMonitorService.updateNode(ftNodeMonitor);

    } else {
      String msg = resultDto == null ? "操作错误" : resultDto.getMessage();
      addMessage(redirectAttributes, msg);
    }
    return "redirect:" + Global.getAdminPath() + "/servicenode/ftServiceNode";
  }

  @RequiresPermissions("servicenode:ftServiceNode:edit")
  @RequestMapping(value = "delete")
  public String delete(FtServiceNode ftServiceNode, HttpServletRequest request, RedirectAttributes redirectAttributes) {

    if (null != ftServiceNode && ftServiceNode.getState().equalsIgnoreCase("1")) {
      addMessage(redirectAttributes, "不能删除正在运行的节点");
      return "redirect:" + Global.getAdminPath() + "/servicenode/ftServiceNode";
    }
    ResultDto<String> resultDto = nodesService.del(ftServiceNode, request);
    if (ResultDtoTool.isSuccess(resultDto) && ftServiceNode != null && ftServiceNode.getName() != null) {
      addMessage(redirectAttributes, "删除节点成功");
      FtNodeMonitor ftNodeMonitor = new FtNodeMonitor();
      ftNodeMonitor.setNode(ftServiceNode.getName());
      ftNodeMonitorService.deleteNode(ftNodeMonitor);

    } else {
      addMessage(redirectAttributes, resultDto.getMessage());
    }
    return "redirect:" + Global.getAdminPath() + "/servicenode/ftServiceNode";
  }

  @RequiresPermissions("servicenode:ftServiceNode:view")
  @RequestMapping(value = "otherConf")
  public String otherConf(FtServiceNode ftServiceNode, HttpServletRequest request, HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {

    if (ftServiceNode.getName() != null) {
      request.getSession().setAttribute("ftServiceNodeOtherConf", ftServiceNode);
    } else {
      ftServiceNode = (FtServiceNode) request.getSession().getAttribute("ftServiceNodeOtherConf");
    }
    if (ftServiceNode.getState().trim().equals("0")) {
      addMessage(redirectAttributes, "节点未连接，请开启客户端后，再次尝试！");
      return "redirect:" + Global.getAdminPath() + "/servicenode/ftServiceNode";
    } else if (ftServiceNode.getState().trim().equals("1")) {
      String getAllStr = MessageFactory.getInstance().nodeParam(new FtNodeParam(), "print");//生成查询报文
      TCPAdapter tcpAdapter = new TCPAdapter();
      ResultDto<String> resultDto = tcpAdapter.invoke(getAllStr, ftServiceNode, String.class);//发送报文
      if (ResultDtoTool.isSuccess(resultDto)) {
        try {
          if (resultDto.getData() != null) {
            model.addAttribute("returnMsg", URLDecoder.decode(resultDto.getData(), "utf-8"));
          } else {
            model.addAttribute("returnMsg", "");
          }
        } catch (IOException e) {
          logger.error("", e);
        }
      } else {
        model.addAttribute("returnMsg", "");
      }
    }
    return "modules/servicenode/ftServiceNodeOtherConf";
  }

  @RequiresPermissions("servicenode:ftServiceNode:view")
  @RequestMapping(value = "listAll")
  public String listAll(FtServiceNode ftServiceNode, HttpServletRequest request, HttpServletResponse response, Model model) {

    ftServiceNode = (FtServiceNode) request.getSession().getAttribute("ftServiceNodeOtherConf");
    String getAllStr = MessageFactory.getInstance().nodes(new FtServiceNode(), "print");//生成查询报文
    TCPAdapter tcpAdapter = new TCPAdapter();
    ResultDto<String> resultDto = tcpAdapter.invoke(getAllStr, ftServiceNode, String.class);//发送报文
    if (ResultDtoTool.isSuccess(resultDto)) {
      try {
        if (resultDto.getData() != null) {
          model.addAttribute("returnMsg", URLDecoder.decode(resultDto.getData(), "utf-8"));
        } else {
          model.addAttribute("returnMsg", "");
        }
      } catch (IOException e) {
        logger.error("", e);
      }
    } else {
      model.addAttribute("returnMsg", "");
    }

    return "modules/servicenode/ftServiceNodeListAll";
  }


  @RequiresPermissions("servicenode:ftServiceNode:view")
  @RequestMapping(value = {"catchFileCfg"})
  @ResponseBody
  public String catchFileCfg(String fileName, HttpServletRequest request) {
    FtServiceNode ftServiceNode = CurrNameNodeHelper.getCurrNameNode(request);
    String getAllStr = null;
    String resultType = null;
    if ((!(null == fileName || fileName.isEmpty())) && fileName.equalsIgnoreCase("all")) {
      fileName = "nodes,user,file,file_clean,components,services_info,crontab" +
          ",file_rename,flow,route,system,vsysmap";
      getAllStr = MessageFactory.getInstance().cfgSync(fileName, "nodeSync", ftServiceNode.getSystemName());//生成查询报文
      TCPAdapter tcpAdapter = new TCPAdapter();
      //发送报文
      ResultDto<String> resultDto = tcpAdapter.invoke(getAllStr, ftServiceNode, String.class);
      String returnMsg = resultDto.getData();
      if (!returnMsg.isEmpty() && returnMsg.contains("true") && returnMsg.contains("false") && !ResultDtoTool.isSuccess(resultDto)) {//NOSONAR
        return returnMsg.replace("true", "同步成功").replace("false", "同步失败").replace("{", "").replace("}", "");
      } else if (!returnMsg.isEmpty() && returnMsg.contains("true") && ResultDtoTool.isSuccess(resultDto)) {
        resultType = "true";
        return resultType;
      } else {
        return returnMsg;
      }
    } else if (fileName != null && fileName.length() > 0) {
      getAllStr = MessageFactory.getInstance().cfgSync(fileName, "nodeSync", ftServiceNode.getSystemName());//生成查询报文
      TCPAdapter tcpAdapter = new TCPAdapter();
      //发送报文
      ResultDto<String> resultDto = tcpAdapter.invoke(getAllStr, ftServiceNode, String.class);
      String returnMsg = resultDto.getData();
      if (returnMsg.contains(":true") && ResultDtoTool.isSuccess(resultDto)) {
        resultType = "true";
      } else if (returnMsg.contains(":false") && !ResultDtoTool.isSuccess(resultDto)) {
        resultType = "false";
      } else {
        resultType = resultDto.getMessage();
      }
    }
    return resultType;
  }


  @RequiresPermissions("servicenode:ftServiceNode:view")
  @RequestMapping(value = {"checkForSys"})
  @ResponseBody
  public String checkForSys(String sysName) {
    List<FtSysInfo> ftSysInfoList = ftSysInfoService.findList(new FtSysInfo());
    String optType = "false";
    for (FtSysInfo fsi : ftSysInfoList) {
      if (fsi.getName().equalsIgnoreCase(sysName) && fsi.getSysNodeModel().contains("ms")) {
        optType = "true";
        break;
      } else if (fsi.getName().equalsIgnoreCase(sysName) && fsi.getSysNodeModel().contains("single")) {
        optType = "isSingle";
      }
    }
    return optType;
  }


  /**
   * 查询当前节点所有任务数量
   *
   * @param ftServiceNode
   * @param ftNodeParam
   * @param optType
   * @param jsonKey
   * @return
   */
  public int queryTaskCount(FtServiceNode ftServiceNode, FtNodeParam ftNodeParam, String optType, String jsonKey) {
    int taskCount = 0;

    String getAllStr = MessageFactory.getInstance().nodeParam(ftNodeParam, optType);//生成查询报文

    TCPAdapter tcpAdapter = new TCPAdapter();
    ResultDto<String> resultDto = tcpAdapter.invoke(getAllStr, ftServiceNode, String.class);//发送报文
    if (ResultDtoTool.isSuccess(resultDto)) {
      JSONObject jsonObject = JSONObject.fromObject(resultDto.getData());

      JSONObject jsonData = jsonObject.getJSONObject(jsonKey);
      Iterator it = jsonData.keys();
      String key = null;
      String value = null;
      while (it.hasNext()) {
        key = (String) it.next();
        value = jsonData.getString(key);
        if (value != null && !"".equals(value.trim())) {
          taskCount += Integer.parseInt(value);
        }
      }
    }
    return taskCount;
  }
}
