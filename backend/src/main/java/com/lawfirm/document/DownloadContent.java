package com.lawfirm.document;

/**
 * 下载内容封装
 */
public record DownloadContent(java.nio.file.Path path, String originalName, String contentType, long size) {
}
