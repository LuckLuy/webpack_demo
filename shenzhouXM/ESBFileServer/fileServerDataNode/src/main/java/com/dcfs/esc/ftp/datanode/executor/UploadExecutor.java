package com.dcfs.esc.ftp.datanode.executor;

import com.dcfs.esb.ftp.common.error.FtpErrCode;
import com.dcfs.esb.ftp.common.error.FtpException;
import com.dcfs.esb.ftp.common.helper.CapabilityDebugHelper;
import com.dcfs.esb.ftp.impls.service.IPCheckService;
import com.dcfs.esb.ftp.servcomm.Test2XML;
import com.dcfs.esc.ftp.comm.chunk.ChunkConfig;
import com.dcfs.esc.ftp.comm.dto.*;
import com.dcfs.esc.ftp.datanode.context.ChannelContext;
import com.dcfs.esc.ftp.datanode.context.ContextBean;
import com.dcfs.esc.ftp.datanode.context.UploadContextBean;
import com.dcfs.esc.ftp.datanode.process.ProcessExecutor;
import com.dcfs.esc.ftp.datanode.process.ProcessHandlerContext;
import com.dcfs.esc.ftp.datanode.process.handler.*;
import com.dcfs.esc.ftp.datanode.process.handler.interfac.ProcessHandler;
import com.dcfs.esc.ftp.datanode.process.handler.interfac.UploadProcessHandler;

import java.lang.reflect.Array;
import java.util.List;

/**
 * Created by mocg on 2017/6/3.
 */
public class UploadExecutor extends BaseBusinessExecutor {

  @SuppressWarnings("unchecked")
  @Override
  protected <T extends BaseDto & RspDto, D extends BaseDto & ReqDto> T invoke0(ChannelContext channelContext, D dto, Class<T> tClass) throws Exception {
    /**
     *    ？？？？ 此处都没有带数据过来？
     */
    if (!(dto instanceof FileUploadDataReqDto)) {
      throw new FtpException(FtpErrCode.FAIL, dto.getNano()
          , "FileUploadDataReqDto#请求对象类型错误#" + dto.getClass().getName());
    }
    BaseDto target;
    FileUploadDataReqDto reqDto = (FileUploadDataReqDto) dto;
    if (log.isTraceEnabled()) {
      log.trace("nano:{}#flowNo:{}#文件上传请求#position:{}", channelContext.getNano(), channelContext.getFlowNo(), reqDto.getPosition());
    }
    //   从服务器信息中取出 客户端信息。
    UploadContextBean cxtBean = channelContext.cxtBean();

    ChunkConfig chunkConfig = channelContext.chunkConfig();

    //流程处理
    /**
     *  此处 获取数据为null ，理论上是应该有值得
     */
    // *****
    ProcessExecutor processExecutor = cxtBean.getProcessExecutor();

    /**
     *  需要点入这里查看，
     *  此处数据为null、其中handlers list 为 0
     */
    if (processExecutor == null) {
      processExecutor = new ProcessExecutor();
      cxtBean.setProcessExecutor(processExecutor); //此处就有数据了。
      addHandlers(processExecutor);
      ProcessHandlerContext processHandlerContext = new ProcessHandlerContext(channelContext);
      cxtBean.setProcessHandlerContext(processHandlerContext);
      processExecutor.start(processHandlerContext);
    }/*else{
      // *****
      addfileHandlers();

    }*/
    ProcessHandlerContext processHandlerContext = cxtBean.getProcessHandlerContext();
    processHandlerContext.setRequestObj(reqDto);
    FileUploadDataRspDto rspDto = new FileUploadDataRspDto();
    processHandlerContext.setResponseObj(rspDto);
    //流程执行
    CapabilityDebugHelper.markCurrTime("uploadExecutor.processExecutor.invoke-before");
    processExecutor.invoke(processHandlerContext, reqDto);
    CapabilityDebugHelper.markCurrTime("uploadExecutor.processExecutor.invoke-after");
    //加工处理返回结果
    rspDto = processHandlerContext.responseObj(FileUploadDataRspDto.class);
    rspDto.setPosition(cxtBean.getPosition());
    //最后一片成功后才有值返回
    if (reqDto.isLastChunk()) rspDto.setFilePath(cxtBean.getSvrFilePath());
    chunkConfig.setLastChunk(reqDto.isLastChunk()); //是否最后一片
    chunkConfig.setEnd(chunkConfig.isEnd() || reqDto.isEnd()); //是否结束
    target = rspDto;
    return (T) target;
  }



  private void addHandlers(ProcessExecutor processExecutor) {
    List<ProcessHandler> handlers = processExecutor.getHandlers();
    String[]   str = Test2XML.getXML("FilesProcesFlow");
    for (String ss :str) {
      switch (ss) {
        case "ResouceCtrlBySetCliHandler":
          filterAndAdd(handlers, new ResouceCtrlBySetCliHandler());
          break;
        case "UploadDecryptHandler":
          filterAndAdd(handlers, new UploadDecryptHandler());
          break;
        case "UploadDecompressHandler":
          filterAndAdd(handlers, new UploadDecompressHandler());
          break;
        case "SaveUploadFileByEsbFileHandler":
          filterAndAdd(handlers, new SaveUploadFileByEsbFileHandler());
          break;
        case "FileDistributeByTailHandler":
          filterAndAdd(handlers, new FileDistributeByTailHandler());
          break;
        case "FileRouteHandler":
          filterAndAdd(handlers, new FileRouteHandler());
          break;
        case "SyncDistributeUploadHandler":
          filterAndAdd(handlers, new SyncDistributeUploadHandler());
          break;
        case "LogOnFinishHandler":
          filterAndAdd(handlers, new LogOnFinishHandler());
          break;
      }
    }

    //filterAndAdd(handlers, new ResouceCtrlBySetCliHandler()); //
   // filterAndAdd(handlers, new UploadDecryptHandler());
    //filterAndAdd(handlers, new UploadDecompressHandler());
    //filterAndAdd(handlers, new SaveUploadFileByEsbFileHandler());
    //filterAndAdd(handlers, new FileDistributeByTailHandler());
    //filterAndAdd(handlers, new FileRouteHandler());
    //filterAndAdd(handlers, new SyncDistributeUploadHandler());
    // filterAndAdd(handlers, new LogOnFinishHandler());



    //filterAndAdd(handlers, new BakOnFinishHandler());//NOSONAR
    //filterAndAdd(handlers, new UploadRecordHandler());//NOSONAR
  }

  private void filterAndAdd(List<ProcessHandler> handlers, ProcessHandler handler) {

    if (handler instanceof UploadProcessHandler) handlers.add(handler);
  }
}
