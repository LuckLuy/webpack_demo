/**
 * Copyright &copy; 2012-2014 <a href="https://github.com/thinkgem/jeesite">JeeSite</a> All rights reserved.
 */
package com.dc.smarteam.modules.monitor.ftnodemonitor.web;

import com.dc.smarteam.cfgmodel.CfgModelConverter;
import com.dc.smarteam.cfgmodel.UserModel;
import com.dc.smarteam.common.adapter.TCPAdapter;
import com.dc.smarteam.common.config.Global;
import com.dc.smarteam.common.json.JsonToEntityFactory;
import com.dc.smarteam.common.json.ResultDto;
import com.dc.smarteam.common.json.ResultDtoTool;
import com.dc.smarteam.common.msggenerator.FtNodeParamMsgGen;
import com.dc.smarteam.common.persistence.Page;
import com.dc.smarteam.common.utils.StringUtils;
import com.dc.smarteam.common.zk.ZkService;
import com.dc.smarteam.cons.GlobalCons;
import com.dc.smarteam.helper.DateHelper;
import com.dc.smarteam.helper.FtNodeMonitorHelper;
import com.dc.smarteam.helper.PageHelper;
import com.dc.smarteam.modules.file.entity.*;
import com.dc.smarteam.modules.file.service.*;
import com.dc.smarteam.modules.monitor.ftnodemonitor.entity.FtNodeAlarmLogMonitor;
import com.dc.smarteam.modules.monitor.ftnodemonitor.entity.FtNodeMonitor;
import com.dc.smarteam.modules.monitor.ftnodemonitor.entity.FtNodeMonitorLog;
import com.dc.smarteam.modules.monitor.ftnodemonitor.entity.FtNodeStateLogMonitor;
import com.dc.smarteam.modules.monitor.ftnodemonitor.service.FtNodeAlarmLogMonitorService;
import com.dc.smarteam.modules.monitor.ftnodemonitor.service.FtNodeMonitorLogService;
import com.dc.smarteam.modules.monitor.ftnodemonitor.service.FtNodeMonitorService;
import com.dc.smarteam.modules.monitor.ftnodemonitor.service.FtNodeStateLogMonitorService;
import com.dc.smarteam.modules.monitor.putfiletomonitor.error.FtpErrCode;
import com.dc.smarteam.modules.servicenode.entity.FtServiceNode;
import com.dc.smarteam.modules.sysinfo.service.FtSysInfoService;
import com.dc.smarteam.modules.user.entity.FtUser;
import com.dc.smarteam.service.ServiceInfoServiceI;
import com.dc.smarteam.service.impl.UserServiceImpl;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * 节点监控Controller
 *
 * @author lvchuan
 * @version 2016-06-27
 */
@Slf4j
@RestController
@RequestMapping(value = "${adminPath}/monitor/FtNodeMonitor")
public class FtNodeMonitorController {
    @Autowired
    @Qualifier("FtNodeMonitorServiceImpl")
    private FtNodeMonitorService ftNodeMonitorService;
    @Autowired
    @Qualifier("BizFileUploadLogServiceImpl")
    private BizFileUploadLogServiceI bizFileUploadLogService;
    @Autowired
    @Qualifier("BizFileDownloadLogServiceImpl")
    private BizFileDownloadLogServiceI bizFileDownloadLogService;
    @Autowired
    @Qualifier("FtSysInfoServiceImpl")
    private FtSysInfoService ftSysInfoService;
    @Autowired
    @Qualifier("FtNodeAlarmLogMonitorServiceImpl")
    private FtNodeAlarmLogMonitorService ftNodeAlarmLogMonitorService;
    @Autowired
    private FtNodeStateLogMonitorService ftNodeStateLogMonitorService;
    @Autowired
    private FtNodeMonitorLogService ftNodeMonitorLogService;
    @Autowired
    private BizFileQueryLogService bizFileQueryLogService;
    @Autowired
    private BizFileFlowNoLogService bizFileFlowNoLogService;
    @Autowired
    @Qualifier("ServiceInfoServiceImpl")
    private ServiceInfoServiceI serviceInfoService;
    @Autowired
    private ArchiveService archiveService;
    @Autowired
    private FileSyncService fileSyncService;
    @Resource(name = "UserServiceImpl")
    private UserServiceImpl userService;
    @Value("${monitorPeriods}")
    private String monitorPeriods;
    @Value("${monitorHistoryDayNum}")
    private String monitorHistoryDayNum;

    public static final String FILE_LAST_PIECE_ERROR = "TBLFTSE0516-文件重定向";

    public static final String FILE_NOT_FOUND_ERROR = "TDCFTSE0502-文件不存在";

    public static final String DATE_FORMAT = "yyyy-MM-dd 00:00:00";

    @ModelAttribute
    public FtNodeMonitor get(@RequestParam(required = false) String id) {
        FtNodeMonitor entity = null;
        if (StringUtils.isNotBlank(id)) {
            entity = ftNodeMonitorService.get(id);
        }
        if (entity == null) {
            entity = new FtNodeMonitor();
        }
        return entity;
    }

    /*@RequiresPermissions("NodeMonitor:ftNodeMonitor:view")*/
    @GetMapping(value = "/list")
    public ResultDto<Page<FtNodeMonitor>> list(FtNodeMonitor ftNodeMonitor, HttpServletRequest request, HttpServletResponse response) {
        Page<FtNodeMonitor> page = ftNodeMonitorService.findPage(new Page<FtNodeMonitor>(request, response), ftNodeMonitor);
        List<FtNodeMonitor> list = page.getList();
        List<FtNodeMonitor> list2 = new ArrayList<>();
        for (FtNodeMonitor ftNodeMonitorTemp : list) {
            if (StringUtils.isNotEmpty(ftNodeMonitor.getNode()) &&
                    !StringUtils.containsIgnoreCase(ftNodeMonitorTemp.getNode(), ftNodeMonitor.getNode())) continue;
            FtNodeMonitorHelper.updateStateByZK(ftNodeMonitorTemp);
            list2.add(ftNodeMonitorTemp);
        }
        page.setList(list2);
        return ResultDtoTool.buildSucceed("success", page);
    }

    /*@RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = {"topo"})
    public String topo() throws Exception {
        return "modules/monitor/nodeMonitor/ftTopo";
    }*/

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = {"allcpu"})
    public String allcpu(Model model) {
        model.addAttribute("monitorPeriods", monitorPeriods);
        return "modules/monitor/nodeMonitor/ftAllCpu";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = {"allstorage"})
    public String allstorage() throws Exception {
        return "modules/monitor/nodeMonitor/ftAllStorage";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = {"allmemeory"})
    public String allmemeory(Model model) {
        model.addAttribute("monitorPeriods", monitorPeriods);
        return "modules/monitor/nodeMonitor/ftAllMemory";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = {"alldisk"})
    public String alldisk() throws Exception {
        return "modules/monitor/nodeMonitor/ftAllDisk";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = {"allflowrate"})
    public String allflowrate(Model model) {
        model.addAttribute("monitorPeriods", monitorPeriods);
        return "modules/monitor/nodeMonitor/ftAllFlowRate";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = {"allnetworkconsume"})
    public String allnetworkconsume(Model model) {
        model.addAttribute("monitorPeriods", monitorPeriods);
        return "modules/monitor/nodeMonitor/ftAllNetworkConsume";
    }


    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = "/monitorlog")
    public ResultDto<Map<String, Object>> monitorlog(FtNodeMonitorLog ftNodeMonitorLog, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<>();
        List<String> systemNameList = ftSysInfoService.findSystemNameList();
        resultMap.put("systemNameList", systemNameList);

        List<String> nodeNameList = ftNodeMonitorService.findNodeNameList();
        resultMap.put("nodeNameList", nodeNameList);

        if (ftNodeMonitorLog.getBeginDate() == null && ftNodeMonitorLog.getEndDate() == null) {
            ftNodeMonitorLog.setBeginDate(DateHelper.getStartDate());
            ftNodeMonitorLog.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        if (ftNodeMonitorLog.getBeginDate() != null) {
            resultMap.put("beginDate", sdf.format(ftNodeMonitorLog.getBeginDate()));
        }
        if (ftNodeMonitorLog.getEndDate() != null) {
            resultMap.put("endDate", sdf.format(ftNodeMonitorLog.getEndDate()));
        }
        Page<FtNodeMonitorLog> page = ftNodeMonitorLogService.findPage(new Page<FtNodeMonitorLog>(request, response), ftNodeMonitorLog);
        ftNodeMonitorLog.setPage(page);
        resultMap.put("page", page);
        return ResultDtoTool.buildSucceed("seccess", resultMap);
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "cpu")
    public String cpu(FtNodeMonitor ftNodeMonitor, Model model) {
        model.addAttribute("ftNodeMonitor", ftNodeMonitor);
        return "modules/monitor/nodeMonitor/ftCpu";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "disk")
    public String disk(FtNodeMonitor ftNodeMonitor, Model model) {
        model.addAttribute("ftNodeMonitor", ftNodeMonitor);
        return "modules/monitor/nodeMonitor/ftDisk";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "memory")
    public String memory(FtNodeMonitor ftNodeMonitor, Model model) {
        model.addAttribute("ftNodeMonitor", ftNodeMonitor);
        return "modules/monitor/nodeMonitor/ftMemory";
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "flowrate")
    public String flowrate(FtNodeMonitor ftNodeMonitor, Model model) {
        model.addAttribute("ftNodeMonitor", ftNodeMonitor);
        return "modules/monitor/nodeMonitor/ftFlowRate";
    }


    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "nodeInfo")
    @ResponseBody
    public FtNodeMonitor getNodeInfo(String nodename) {
        FtNodeMonitor ftNodeMonitor = new FtNodeMonitor();
        ftNodeMonitor.setNode(nodename);
        return ftNodeMonitorService.getNode(ftNodeMonitor);
    }


    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "nodeInfoLog")
    @ResponseBody
    public List<FtNodeMonitor> getNodeInfoLog(String node) {
        FtNodeMonitor ftNodeMonitor = new FtNodeMonitor();
        ftNodeMonitor.setNode(node);
        List<FtNodeMonitor> nodelogList = ftNodeMonitorService.getNodeLog(ftNodeMonitor);
        List<FtNodeMonitor> nodelogListForNode = new ArrayList<>();
        int i = 0;
        for (FtNodeMonitor fnm : nodelogList) {
            if (fnm.getNode().equalsIgnoreCase(node) && i < 200) {
                nodelogListForNode.add(fnm);
                i++;
            }
        }
        return nodelogListForNode;
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "network")
    @ResponseBody
    public String network(String node, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        FtServiceNode ftServiceNode = new FtServiceNode();
        Map<String, JsonObject> dataNodeMap = ZkService.getInstance().getDataNodeMap();
        for (String dnIpPort : dataNodeMap.keySet()) {
            JsonObject jsonObject = dataNodeMap.get(dnIpPort);
            String dataNodeName = jsonObject.get("nodeName").getAsString();
            if (node != null && node.equalsIgnoreCase(dataNodeName)) {
                String[] split = dnIpPort.split(":");
                ftServiceNode.setIpAddress(split[0]);
                ftServiceNode.setCmdPort(split[1]);
                ftServiceNode.setName(node);
                String getStr = FtNodeParamMsgGen.currResource(FtNodeParamMsgGen.NETWORK);
                TCPAdapter tcpAdapter = new TCPAdapter();//NOSONAR
                ResultDto<String> resultDto = tcpAdapter.invoke(getStr, ftServiceNode, String.class);//发送报文
                if (ResultDtoTool.isSuccess(resultDto)) {
                    String data = resultDto.getData();
                    JSONObject json = JSONObject.fromObject(data);
                    return JsonToEntityFactory.getInstance().getString(json, "networkSpeed");
                }
            }
        }
        return GlobalCons.GLOBAL_ZERO;
    }

    /*@RequiresPermissions("NodeMonitor:ftNodeMonitor:view")*/
    @GetMapping(value = "/nodeInfoLogOneHour")
    public ResultDto<List<FtNodeMonitor>> nodeInfoLogOneHour(@RequestParam(name = "node", required = false) String node) throws Exception {
        Date date = new Date();
        monitorHistoryDayNum = monitorHistoryDayNum == null ? "1" : monitorHistoryDayNum;
        long historyDate = date.getTime() - Integer.parseInt(monitorHistoryDayNum) * 3600000 * 24;

        Map<String, Object> map = new HashMap<>();
        map.put("beginDate", new Date(historyDate));
        map.put("endDate", date);
        map.put("node", node);
        long l1 = System.currentTimeMillis();
        List<FtNodeMonitor> nodelogList = ftNodeMonitorService.getNodeLogList(map);
        long l2 = System.currentTimeMillis();

        return ResultDtoTool.buildSucceed("success", nodelogList);
    }


    /*@RequiresPermissions("NodeMonitor:ftNodeMonitor:view")*/
    @GetMapping(value = "/allnodeInfo")
    public ResultDto<List<FtNodeMonitor>> getAllNodeInfo() {
        List<FtNodeMonitor> list = ftNodeMonitorService.findList(new FtNodeMonitor());
        for (FtNodeMonitor ftNodeMonitor : list) {
            FtNodeMonitorHelper.updateStateByZK(ftNodeMonitor);
        }
        return ResultDtoTool.buildSucceed("success", list);
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "allLiveDatanode")
    @ResponseBody
    public List<FtNodeMonitor> getAllLiveNodeInfo() {
        List<FtNodeMonitor> list = ftNodeMonitorService.findList(new FtNodeMonitor());
        List<FtNodeMonitor> liveDNList = new ArrayList<>();
        Map<String, JsonObject> dataNodeMap = ZkService.getInstance().getDataNodeMap();
        for (FtNodeMonitor ftNodeMonitor : list) {
            String node = ftNodeMonitor.getNode();
            for (String ipPort : dataNodeMap.keySet()) {
                JsonObject jsonObject = dataNodeMap.get(ipPort);
                String dataNodeNameTemp = jsonObject.get("nodeName").getAsString();
                if (dataNodeNameTemp.equals(node)) {
                    liveDNList.add(ftNodeMonitor);
                    break;
                }
            }
        }
        return liveDNList;
    }

    /*@RequiresPermissions("NodeMonitor:ftNodeMonitor:view")
    @RequestMapping(value = "/form")
    public String form(FtNodeMonitor ftNodeMonitor, Model model) {
        model.addAttribute("ftNodeMonitor", ftNodeMonitor);
        return "modules/monitor/nodeMonitor/ftNodeMonitorForm";
    }*/

    /*@RequiresPermissions("NodeMonitor:ftNodeMonitor:edit")*/
    @PutMapping(value = "/save")
    public ResultDto<String> save(@RequestBody @Valid FtNodeMonitor ftNodeMonitor) {
        ftNodeMonitorService.setThreshold(ftNodeMonitor);
        if (log.isInfoEnabled()) {
            log.info("{} instance {} was updated.", FtNodeMonitor.class.getSimpleName(), ftNodeMonitor.getId());
        }
        return ResultDtoTool.buildSucceed("success");
    }

    @RequiresPermissions("NodeMonitor:ftNodeMonitor:edit")
    @RequestMapping(value = "delete")
    public String delete(FtNodeMonitor ftNodeMonitor, RedirectAttributes redirectAttributes) {
        ftNodeMonitorService.delete(ftNodeMonitor);
        //addMessage(redirectAttributes, "删除文件清理成功");
        return "redirect:" + Global.getAdminPath() + "/NodeMonitor/ftNodeMonitor/?repage";
    }


    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = "/monitorlogForUpload")
    public ResultDto<Map<String, Object>> monitorlogForUpload(
            BizFileUploadLog bizFileUploadLog, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<>();
        List<String> systemNameList = ftSysInfoService.findSystemNameList();
        resultMap.put("systemNameList", systemNameList);

        List<String> nodeNameList = ftNodeMonitorService.findNodeNameList();
        resultMap.put("nodeNameList", nodeNameList);
        if (bizFileUploadLog.getBeginDate() == null && bizFileUploadLog.getEndDate() == null) {
            bizFileUploadLog.setBeginDate(DateHelper.getStartDate());
            bizFileUploadLog.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (bizFileUploadLog.getBeginDate() != null) {
            resultMap.put("beginDate", sdf.format(bizFileUploadLog.getBeginDate()));
        }
        if (bizFileUploadLog.getEndDate() != null) {
            resultMap.put("endDate", sdf.format(bizFileUploadLog.getEndDate()));
        }
        Page<BizFileUploadLog> page = bizFileUploadLogService.findPage(new Page<BizFileUploadLog>(request, response), bizFileUploadLog);
        List<BizFileUploadLog> bizFileUploadLogList = page.getList();
        for (BizFileUploadLog uploadLog : bizFileUploadLogList) {
            if (uploadLog.isLastPiece() && uploadLog.isSuss()) {
                uploadLog.setUploadSuss("true");
            } else {
                uploadLog.setUploadSuss("false");
            }
            if (uploadLog.getErrCode() != null) {
                String uploadmsg = FtpErrCode.getCodeMsg(uploadLog.getErrCode());
                uploadLog.setUploadErrMsg(uploadmsg);
            } else if (!uploadLog.isLastPiece() && uploadLog.isSuss()) {
                uploadLog.setUploadErrMsg(FILE_LAST_PIECE_ERROR);
            }

        }
        page.setList(bizFileUploadLogList);
        resultMap.put("page", page);
        resultMap.put("ftServiceInfoList", serviceInfoService.getFtServiceInfoList());
        return ResultDtoTool.buildSucceed("success", resultMap);
    }

    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = "/monitorlogForUploadOne")
    public ResultDto<? extends Object> monitorlogForUploadOne(@RequestBody BizFileUploadLog bizFileUploadLog) {
        BizFileUploadLog bizFileUploadLogTemp = null;
        try {
            bizFileUploadLogTemp = bizFileUploadLogService.get(bizFileUploadLog);
            if ("1".equals(bizFileUploadLogTemp.getFileRenameCtrl())) {
                bizFileUploadLogTemp.setFileRenameCtrl("重命名");
            } else {
                bizFileUploadLogTemp.setFileRenameCtrl("未重命名");
            }
        } catch (Exception e) {
            return ResultDtoTool.buildError("查询文件信息失败！");
        }

        return ResultDtoTool.buildSucceed("success", bizFileUploadLogTemp);
    }

    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = "/monitorlogForDownload")
    public ResultDto<Map<String, Object>> monitorlogForDownload(BizFileDownloadLog bizFileDownloadLog, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<>();
        List<String> systemNameList = ftSysInfoService.findSystemNameList();
        resultMap.put("systemNameList", systemNameList);

        List<String> nodeNameList = ftNodeMonitorService.findNodeNameList();
        resultMap.put("nodeNameList", nodeNameList);
        if (bizFileDownloadLog.getBeginDate() == null && bizFileDownloadLog.getEndDate() == null) {
            bizFileDownloadLog.setBeginDate(DateHelper.getStartDate());
            bizFileDownloadLog.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (bizFileDownloadLog.getBeginDate() != null) {
            resultMap.put("beginDate", sdf.format(bizFileDownloadLog.getBeginDate()));
        }
        if (bizFileDownloadLog.getEndDate() != null) {
            resultMap.put("endDate", sdf.format(bizFileDownloadLog.getEndDate()));
        }
        Page<BizFileDownloadLog> page = bizFileDownloadLogService.findPage(new Page<BizFileDownloadLog>(request, response), bizFileDownloadLog);
        List<BizFileDownloadLog> bizFileDownloadLogServiceList = page.getList();

        for (BizFileDownloadLog downlog : bizFileDownloadLogServiceList) {
            if (downlog.isLastPiece() && downlog.isSuss()) {
                downlog.setDownloadSuss("true");
            } else {
                downlog.setDownloadSuss("false");
            }
            if (downlog.getErrCode() != null) {
                String downmsg = FtpErrCode.getCodeMsg(downlog.getErrCode());
                downlog.setDownerrMsg(downmsg);
            } else if (!downlog.isLastPiece() && downlog.isSuss()) {
                downlog.setDownerrMsg(FILE_LAST_PIECE_ERROR);
            }

        }
        page.setList(bizFileDownloadLogServiceList);
        resultMap.put("page", page);
        resultMap.put("ftServiceInfoList", serviceInfoService.getFtServiceInfoList());
        return ResultDtoTool.buildSucceed("success", resultMap);
    }

    @RequiresPermissions("NodeMonitor:monitorHistory:view")
    @RequestMapping(value = {"monitorlogForQuery"})
    public String monitorlogForQuery(BizFileQueryLog bizFileQueryLog, HttpServletRequest request, HttpServletResponse response, Model model) {

        List<String> systemNameList = ftSysInfoService.findSystemNameList();
        model.addAttribute("systemNameList", systemNameList);

        List<String> nodeNameList = ftNodeMonitorService.findNodeNameList();
        model.addAttribute("nodeNameList", nodeNameList);
        if (bizFileQueryLog.getBeginDate() == null && bizFileQueryLog.getEndDate() == null) {
            bizFileQueryLog.setBeginDate(DateHelper.getStartDate());
            bizFileQueryLog.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (bizFileQueryLog.getBeginDate() != null) {
            model.addAttribute("beginDate", sdf.format(bizFileQueryLog.getBeginDate()));
        }
        if (bizFileQueryLog.getEndDate() != null) {
            model.addAttribute("endDate", sdf.format(bizFileQueryLog.getEndDate()));
        }
        Page<BizFileQueryLog> page = bizFileQueryLogService.findPage(new Page<BizFileQueryLog>(request, response), bizFileQueryLog);
        List<BizFileQueryLog> bizFileQueryLogList = page.getList();
        Iterator<BizFileQueryLog> bizFileQueryLogIterator = bizFileQueryLogList.iterator();
        while (bizFileQueryLogIterator.hasNext()) {
            BizFileQueryLog queryLog = bizFileQueryLogIterator.next();
            if (queryLog.getUperrCode() != null || queryLog.getDownerrCode() != null) {
                String upmsg = FtpErrCode.getCodeMsg(queryLog.getUperrCode());
                String downmsg = FtpErrCode.getCodeMsg(queryLog.getDownerrCode());
                queryLog.setDownerrMsg(downmsg);
                queryLog.setUperrMsg(upmsg);
            } else if (!queryLog.isUplastPiece() && queryLog.isUpsuss()) {
                queryLog.setUperrMsg(FILE_LAST_PIECE_ERROR);
            } else if (!queryLog.isDownlastPiece() && queryLog.isDownsuss()
                    && queryLog.isUplastPiece() && queryLog.isUpsuss()
            ) {
                queryLog.setDownerrMsg(FILE_LAST_PIECE_ERROR);
            }

        }

        model.addAttribute("ftServiceInfoList", serviceInfoService.getFtServiceInfoList());
        model.addAttribute("ftUserInfoList", getftUserinfolist());
        page.setList(bizFileQueryLogList);
        model.addAttribute("page", page);
        return "modules/monitor/nodeMonitor/ftMonitorlogForQuery";
    }

    @RequiresPermissions("NodeMonitor:monitorHistory:view")
    @RequestMapping(value = {"monitorlogForguidang"})
    public String monitorlogForguidang(Archive archive, HttpServletRequest request, HttpServletResponse response, Model model) {

        List<String> userNameList = archiveService.findUserNameList();
        model.addAttribute("userNameList", userNameList);

        List<String> trancodeList = archiveService.findTranCodeList();
        model.addAttribute("trancodeList", trancodeList);
        if (archive.getBeginDate() == null && archive.getEndDate() == null) {
            archive.setBeginDate(DateHelper.getStartDate());
            archive.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (archive.getBeginDate() != null) {
            model.addAttribute("beginDate", sdf.format(archive.getBeginDate()));
        }
        if (archive.getEndDate() != null) {
            model.addAttribute("endDate", sdf.format(archive.getEndDate()));
        }
        Page<Archive> page = archiveService.findPage(new Page<Archive>(request, response), archive);
        List<Archive> ArchiveServiceList = page.getList();
        for (Archive downlog : ArchiveServiceList) {
            if ("Y".equals(downlog.getUploadFlag()) && "Y".equals(downlog.getClearFlag())) {
                downlog.setUploadFlag("true");
            } else {
                downlog.setUploadFlag("false");
            }
        }
        page.setList(ArchiveServiceList);
        model.addAttribute("page", page);
        model.addAttribute("ftServiceInfoList", serviceInfoService.getFtServiceInfoList());
        return "modules/monitor/nodeMonitor/monitorlogForguidang";
    }

    @RequiresPermissions("NodeMonitor:monitorHistory:view")
    @RequestMapping(value = {"monitorlogForFileSync"})
    public String monitorlogForFileSync(FileSync fileSync, HttpServletRequest request, HttpServletResponse response, Model model) {

        List<String> nodeNameList = fileSyncService.findNodeNameList();
        model.addAttribute("nodeNameList", nodeNameList);

        List<String> trancodeList = fileSyncService.findTranCodeList();
        model.addAttribute("trancodeList", trancodeList);
        if (fileSync.getBeginDate() == null && fileSync.getEndDate() == null) {
            fileSync.setBeginDate(DateHelper.getStartDate());
            fileSync.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (fileSync.getBeginDate() != null) {
            model.addAttribute("beginDate", sdf.format(fileSync.getBeginDate()));
        }
        if (fileSync.getEndDate() != null) {
            model.addAttribute("endDate", sdf.format(fileSync.getEndDate()));
        }
        Page<FileSync> page = fileSyncService.findPage(new Page<FileSync>(request, response), fileSync);
        List<FileSync> FileServiceList = page.getList();
        for (FileSync downlog : FileServiceList) {
            if (downlog.getErrCode() == null) {
                downlog.setSTATE("true");
            } else {
                downlog.setSTATE("false");
            }
        }
        page.setList(FileServiceList);
        model.addAttribute("page", page);
        model.addAttribute("ftServiceInfoList", serviceInfoService.getFtServiceInfoList());
        return "modules/monitor/nodeMonitor/monitorlogForFileSync";
    }

    @RequiresPermissions("NodeMonitor:monitorHistory:view")
    @RequestMapping(value = {"monitorlogForFlow"})
    public String monitorlogForFlow(BizFileFlowNoLog bizFileFlowNoLog, HttpServletRequest request, HttpServletResponse response, Model model) {
        if (bizFileFlowNoLog.getBeginDate() == null && bizFileFlowNoLog.getEndDate() == null) {
            bizFileFlowNoLog.setBeginDate(DateHelper.getStartDate());
            bizFileFlowNoLog.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (bizFileFlowNoLog.getBeginDate() != null) {
            model.addAttribute("beginDate", sdf.format(bizFileFlowNoLog.getBeginDate()));
        }
        if (bizFileFlowNoLog.getEndDate() != null) {
            model.addAttribute("endDate", sdf.format(bizFileFlowNoLog.getEndDate()));
        }
        Page<BizFileFlowNoLog> page = bizFileFlowNoLogService.findPage(new Page<BizFileFlowNoLog>(request, response), bizFileFlowNoLog);
        model.addAttribute("page", page);
        model.addAttribute("ftServiceInfoList", serviceInfoService.getFtServiceInfoList());
        model.addAttribute("ftUserInfoList", getftUserinfolist());
        return "modules/monitor/nodeMonitor/ftMonitorlogForFlowNo";
    }

    public List getftUserinfolist() {
        ResultDto<List<UserModel.UserInfo>> userDto = userService.listAll();
        List<FtUser> ftUserList = new ArrayList<FtUser>();
        if (ResultDtoTool.isSuccess(userDto)) {
            List<UserModel.UserInfo> userInfos = userDto.getData();
            for (UserModel.UserInfo userInfo : userInfos) {
                FtUser user = new FtUser();
                CfgModelConverter.convertTo(userInfo, user);
                ftUserList.add(user);
            }
        }
        return ftUserList;
    }

    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = "/monitorlogForDownloadOne")
    public ResultDto<? extends Object> monitorlogForDownloadOne(@RequestBody BizFileDownloadLog bizFileDownloadLog) {
        BizFileDownloadLog bizFileDownloadLogTemp = null;
        try {
            bizFileDownloadLogTemp = bizFileDownloadLogService.get(bizFileDownloadLog);
            if ("1".equals(bizFileDownloadLogTemp.getFileRenameCtrl())) {
                bizFileDownloadLogTemp.setFileRenameCtrl("重命名");
            } else {
                bizFileDownloadLogTemp.setFileRenameCtrl("未重命名");
            }
        } catch (Exception e) {
            //addMessage(redirectAttributes, "查询文件信息失败！详情：" + e.getMessage());
            return ResultDtoTool.buildError("查询文件信息失败");
        }
        return ResultDtoTool.buildSucceed("success", bizFileDownloadLogTemp);
    }

    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = "/monitorlogForUpAndDownTotal")
    public ResultDto<Map<String, Object>> monitorlogForUpAndDownTotal(
            @RequestParam(name = "beginDate", required = false) Date startDate,
            @RequestParam(name = "endDate", required = false) Date endDate) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        Map<String, Object> resultMap = new HashMap<>();
        final String persentsDef = "0.00%";
        Map<String, Object> map = new HashMap<>();
        if (startDate == null && endDate == null) {
            startDate = DateHelper.getStartDate();
            endDate = DateHelper.getEndDate();
        }

        if (startDate != null) {
            resultMap.put("beginDate", startDate);
        }
        if (endDate != null) {
            resultMap.put("endDate", endDate);
        }
        map.put("beginDate", startDate);
        map.put("endDate", endDate);
        List<BizFileUploadLog> bizfileuploadlist = bizFileUploadLogService.findListByTimeByFail(map);
        List<BizFileUploadLog> uplist = new ArrayList<>();
        for (BizFileUploadLog uplog : bizfileuploadlist) {
            if (uplog.getFlowNo() == null || uplog.getUname() == null) continue;
            Map<String, Object> map1 = new HashMap<>();
            map1.put("beginDate", startDate);
            map1.put("endDate", endDate);
            map1.put("flowNo", uplog.getFlowNo());
            map1.put("uname", uplog.getUname());
            long upsusscount = bizFileUploadLogService.findUpsussByflowNoAndUname(map1);
            if (upsusscount != 0) {
                uplist.add(uplog);
            }
        }
        bizfileuploadlist.removeAll(uplist);
        //经过滤后的上传失败数
        long realuploadfail = bizfileuploadlist.size();
        Long listByTimeForUploadBySucc = bizFileUploadLogService.findListByTimeBySucc(map);
        resultMap.put("totalListForUploadSuss", listByTimeForUploadBySucc);
        resultMap.put("totalListForUploadFail", realuploadfail);
        long listByTimeForUploadCount = realuploadfail + listByTimeForUploadBySucc;
        resultMap.put("totalListForUpload", listByTimeForUploadCount);
        Double divideUploadNo = (double) (listByTimeForUploadBySucc) / (double) (listByTimeForUploadCount);
        DecimalFormat df = new DecimalFormat(persentsDef);
        String endNo = df.format(divideUploadNo);
        if (divideUploadNo.isNaN() || endNo.equalsIgnoreCase("NaN")) {
            endNo = persentsDef;
        }
        resultMap.put("totalListForUploadPerc", endNo);
        List<BizFileDownloadLog> bizFileDownloadLogList = bizFileDownloadLogService.findDownListByFail(map);
        List<BizFileDownloadLog> downlist = new ArrayList<>();
        for (BizFileDownloadLog dowload : bizFileDownloadLogList) {
            if (dowload.getFlowNo() == null || dowload.getUname() == null) continue;
            Map<String, Object> map2 = new HashMap<>();
            map2.put("beginDate", startDate);
            map2.put("endDate", endDate);
            map2.put("flowNo", dowload.getFlowNo());
            map2.put("uname", dowload.getUname());

            long downsusscount = bizFileDownloadLogService.finddownloadsussByflowNoAndUname(map2);
            if (downsusscount != 0) {
                downlist.add(dowload);
            }
        }
        bizFileDownloadLogList.removeAll(downlist);
        //经过滤后的下载失败数
        long realdownloadfail = bizFileDownloadLogList.size();
        Long listByTimeForDownloadBySucc = bizFileDownloadLogService.findListByTimeBySucc(map);
        resultMap.put("totalListForDownloadSuss", listByTimeForDownloadBySucc);
        resultMap.put("totalListForDownloadFail", realdownloadfail);
        Long listByTimeForDownloadCount = realdownloadfail + listByTimeForDownloadBySucc;
        resultMap.put("totalListForDownload", listByTimeForDownloadCount);
        Double divideDownloadNo = (double) (listByTimeForDownloadBySucc) / (double) (listByTimeForDownloadCount);
        String endNo2 = df.format(divideDownloadNo);
        if (divideDownloadNo.isNaN() || endNo2.equalsIgnoreCase("NaN")) {
            endNo2 = persentsDef;
        }
        resultMap.put("totalListForDownloadPerc", endNo2);

        return ResultDtoTool.buildSucceed("success", resultMap);
    }

    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = {"/monitorLogForNodeAlarm"})
    public ResultDto<Map<String, Object>> monitorLogForNodeAlarm(FtNodeAlarmLogMonitor ftNodeAlarmLogMonitor, HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<>();
        List<String> nodeNameList = ftNodeMonitorService.findNodeNameList();
        resultMap.put("nodeNameList", nodeNameList);
        if (ftNodeAlarmLogMonitor.getBeginDate() == null && ftNodeAlarmLogMonitor.getEndDate() == null) {
            ftNodeAlarmLogMonitor.setBeginDate(DateHelper.getStartDate());
            ftNodeAlarmLogMonitor.setEndDate(DateHelper.getEndDate());
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (ftNodeAlarmLogMonitor.getBeginDate() != null) {
            resultMap.put("beginDate", sdf.format(ftNodeAlarmLogMonitor.getBeginDate()));
        }
        if (ftNodeAlarmLogMonitor.getEndDate() != null) {
            resultMap.put("endDate", sdf.format(ftNodeAlarmLogMonitor.getEndDate()));
        }

        Page<FtNodeAlarmLogMonitor> page = ftNodeAlarmLogMonitorService.findPage(new Page<FtNodeAlarmLogMonitor>(request, response), ftNodeAlarmLogMonitor);
        ftNodeAlarmLogMonitor.setPage(page);
        resultMap.put("page", page);
        return ResultDtoTool.buildSucceed("success", resultMap);
    }

    @RequiresPermissions("NodeMonitor:monitorHistory:view")
    @RequestMapping(value = {"monitorLogForNodeState"})
    public String monitorLogForNodeState(FtNodeStateLogMonitor ftNodeStateLogMonitor, HttpServletRequest request, HttpServletResponse response, Model model) {

        List<String> nodeNameList = ftNodeMonitorService.findNodeNameList();
        model.addAttribute("nodeNameList", nodeNameList);

        List<String> nodeTypeList = ftNodeStateLogMonitorService.findNodeTypeList();
        model.addAttribute("nodeTypeList", nodeTypeList);

        Page<FtNodeStateLogMonitor> page = ftNodeStateLogMonitorService.findPage(new Page<FtNodeStateLogMonitor>(request, response), ftNodeStateLogMonitor);
        ftNodeStateLogMonitor.setPage(page);
        model.addAttribute("page", page);
        return "modules/monitor/nodeMonitor/ftMonitorlogForNodeState";
    }


    /*@RequiresPermissions("NodeMonitor:monitorHistory:view")*/
    @GetMapping(value = {"/monitorlogForAlarmAndStateTotal"})
    public ResultDto<Map<String, Object>> monitorlogForAlarmAndStateTotal(
            @RequestParam(name = "beginDate", required = false) Date beginDate,
            @RequestParam(name = "endDate", required = false) Date endDate,
            HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Map<String, Object> map = new HashMap<String, Object>();
        if (beginDate == null && endDate == null) {

            beginDate = DateHelper.getStartDate();
            endDate = DateHelper.getEndDate();
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        if (beginDate != null) {
            resultMap.put("beginDate", sdf.format(beginDate));
        }
        if (endDate != null) {
            resultMap.put("endDate", sdf.format(endDate));
        }
        map.put("beginDate", beginDate);
        map.put("endDate", endDate);
        List<Map<String, String>> alarmOftenRecordList = ftNodeAlarmLogMonitorService.findAlarmOftenRecord(map);
        PageHelper.getInstance().getPage(null, request, response, resultMap, alarmOftenRecordList);
        return ResultDtoTool.buildSucceed("success", resultMap);
    }

}