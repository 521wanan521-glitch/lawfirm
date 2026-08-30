<script>
export default {
  onLaunch() {},
  onShow() {}
}
</script>

<style lang="scss">
/* 全局样式 */
page {
  background-color: #f5f6f8;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  color: #1f2329;
  font-size: 28rpx;
  line-height: 1.5;
}

.flex {
  display: flex;
  align-items: center;
}
.flex-between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 主题色 */
.theme-color {
  color: #2f6fed;
}
.theme-bg {
  background-color: #2f6fed;
}

/* 通用卡片 */
.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin: 0 24rpx 20rpx;
  box-shadow: 0 2rpx 12rpx rgba(31, 35, 41, 0.04);
}
</style>
