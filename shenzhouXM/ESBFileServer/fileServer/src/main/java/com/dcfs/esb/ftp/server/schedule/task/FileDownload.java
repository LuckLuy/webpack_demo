package com.dcfs.esb.ftp.server.schedule.task;

import com.dcfs.esb.ftp.server.file.EsbFileManager;
import com.dcfs.esb.ftp.server.schedule.LoopTask;
import com.dcfs.esb.ftp.server.system.IProtocol;
import com.dcfs.esb.ftp.server.system.ProtocolFactory;
import com.dcfs.esb.ftp.server.system.SystemInfo;
import com.dcfs.esb.ftp.server.system.SystemManage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class FileDownload implements LoopTask {
    private static final Logger log = LoggerFactory.getLogger(FileDownload.class);
    private Map<String, String> params;

    public void execute() throws Exception {
        String pathType = params.get("pathType");//local -localFile的值是操作系统的绝对路径 node -localFile的值是当前节点上的绝对路径
        String localFile = params.get("localFile");
        String remoteFile = params.get("remoteFile");
        String destination = params.get("destination");
        String localAbsPath = null;
        if ("local".equals(pathType)) {
            localAbsPath = localFile;
        } else if ("node".equals(pathType)) {
            localAbsPath = EsbFileManager.getInstance().getFileAbsolutePath(localFile);
        }
        SystemInfo systemInfo = SystemManage.getInstance().getSystemInfo(destination);
        if (systemInfo == null) {
            log.error("未知的文件上传目标系统:{}", destination);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            log.info("文件下载中:{}", remoteFile);
            IProtocol protocol = ProtocolFactory.getProtocol(systemInfo, localAbsPath, remoteFile);
            protocol.download();
        } catch (Exception e) {
            log.error("文件下载失败:{}", remoteFile, e);
        } finally {
            long end = System.currentTimeMillis();
            log.info("文件下载结束,总耗时{}", (end - start));
        }
    }

    public void init(Map<String, String> params) {
        this.params = params;
    }

}
