package com.zzzmode.appopsx;

/**
 * 无法连接到 OpsxService 时抛出，{@link #getReason()} 说明原因，供 UI 给出对应提示。
 */
public class ServiceUnavailableException extends Exception {

  public enum Reason {
    /** Shizuku 没有运行（未安装或未启动） */
    SHIZUKU_NOT_RUNNING,
    /** Shizuku 版本过低（低于 v11） */
    SHIZUKU_UNSUPPORTED,
    /** 用户还没有授予本应用 Shizuku 权限 */
    PERMISSION_DENIED,
    /** Shizuku 没能在超时时间内拉起 OpsxService */
    BIND_TIMEOUT,
  }

  private final Reason reason;

  ServiceUnavailableException(Reason reason) {
    super(reason.name());
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }
}
