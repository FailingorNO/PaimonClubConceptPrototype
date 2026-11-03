package com.paimonclub.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS 应用入口：所有 REST 接口挂在 /api 下。
 */
@ApplicationPath("/api")
public class GcApplicationConfig extends Application {
}
