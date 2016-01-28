package com.wobiancao.getluckymomenyeasy.presenter;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Parcelable;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.wobiancao.getluckymomenyeasy.base.BasePresenter;
import com.wobiancao.getluckymomenyeasy.iview.IWechatView;

import java.util.List;

/**
 * Created by xy on 16/1/27.
 */
public class WeChatHouSaiLeiPresenter extends BasePresenter<IWechatView> {
    private static final String WECHAT_VIEW_SELF_CH = "查看红包";
    private static final String WECHAT_VIEW_OTHERS_CH = "领取红包";
    private static final String WECHAT_BETTER_LUCK_CH = "手慢了";
    private static final String WECHAT_BETTER_LUCK_EN = "Better luck next time!";
    private static final String WECHAT_DETAILS_EN = "Details";
    private static final String WECHAT_DETAILS_CH = "红包详情";
    private static final String WECHAT_EXPIRES_CH = "红包已失效";
    private static final String WECHAT_EXPIRES_OVER = "红包派完了";
    private final static String WECHAT_NOTIFICATION_TIP = "[微信红包]";



    @Override
    public void attachIView(IWechatView iHouSaiLeiView) {
        iv = iHouSaiLeiView;
    }
    /**服务接入**/
    public void accessibilityEvent(AccessibilityEvent event) {
        iv.setRootNodeInfo(event.getSource());
        if (iv.getRootNodeInfo() == null) {
            return;
        }
        iv.setReceiveNode(null);
        iv.setUnpackNode(null);
        if (iv.isMutex()) {
            if (watchNotifications(event)) {
                return;
            }
            if (watchList(event)){
                return;
            }
        }

    }

    public void checkNodeInfo() {
        AccessibilityNodeInfo rootNodeInfo = iv.getRootNodeInfo();
        if (rootNodeInfo == null) {
            return;
        }
         /* 聊天会话窗口，遍历节点匹配“领取红包”和"查看红包" */
        List<AccessibilityNodeInfo> nodes1 = findAccessibilityNodeInfosByTexts(rootNodeInfo, new String[]{
                WECHAT_VIEW_OTHERS_CH,
                WECHAT_VIEW_SELF_CH});

        if (!nodes1.isEmpty()) {
            AccessibilityNodeInfo targetNode = nodes1.get(nodes1.size() - 1);
            if (signature.generateSignature(targetNode)) {
                iv.setLuckyMoneyReceived(true);
                iv.setReceiveNode(targetNode);
            }
            return;
        }
        /* 戳开红包，红包还没抢完，遍历节点匹配“拆红包” */
        AccessibilityNodeInfo node2 = (rootNodeInfo.getChildCount() > 3) ? rootNodeInfo.getChild(3) : null;
        if (node2 != null && node2.getClassName().equals("android.widget.Button")) {
            iv.setUnpackNode(node2);
            iv.setNeedUnpack(true);
            return;
        }
//
//        /* 戳开红包，红包已被抢完，遍历节点匹配“红包详情”和“手慢了” */
        if (iv.isLuckyMoneyPicked()) {
            List<AccessibilityNodeInfo> nodes3 = findAccessibilityNodeInfosByTexts(rootNodeInfo, new String[]{
                    WECHAT_BETTER_LUCK_CH,
                    WECHAT_DETAILS_CH,
                    WECHAT_BETTER_LUCK_EN,
                    WECHAT_DETAILS_EN,
                    WECHAT_EXPIRES_OVER,
                    WECHAT_EXPIRES_CH});
            if (!nodes3.isEmpty()) {
                iv.setNeedBack(true);
                iv.setLuckyMoneyPicked(false);
            }
        }
    }

    public void doAction() {

//         /* 如果已经接收到红包并且还没有戳开 */
        AccessibilityNodeInfo mReceiveNode = iv.getReceiveNode();
        if (iv.isLuckyMoneyPicked() && iv.isLuckyMoneyReceived() && mReceiveNode != null){
            iv.setMutex(true);
            AccessibilityNodeInfo cellNode = iv.getReceiveNode();
            cellNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            iv.setLuckyMoneyReceived(false);
            iv.setLuckyMoneyPicked(true);
        }
        /* 如果戳开但还未领取 */
        if (iv.isNeedUnpack() && (iv.getUnpackNode() != null)) {
            AccessibilityNodeInfo cellNode = iv.getUnpackNode();
            cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            iv.setNeedUnpack(false);
        }
        if (iv.isNeedBack()) {
            iv.needBack();
        }
    }

    private boolean watchNotifications(AccessibilityEvent event) {
        // Not a notification
        if (event.getEventType() != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED)
            return false;

        // Not a hongbao
        String tip = event.getText().toString();
        if (!tip.contains(WECHAT_NOTIFICATION_TIP)) return true;

        Parcelable parcelable = event.getParcelableData();
        if (parcelable instanceof Notification) {
            Notification notification = (Notification) parcelable;
            try {
                notification.contentIntent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean watchList(AccessibilityEvent event) {
        // Not a message
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.getSource() == null)
            return false;

        List<AccessibilityNodeInfo> nodes = event.getSource().findAccessibilityNodeInfosByText(WECHAT_NOTIFICATION_TIP);
        if (!nodes.isEmpty()) {
            AccessibilityNodeInfo nodeToClick = nodes.get(0);
            CharSequence contentDescription = nodeToClick.getContentDescription();
            if (contentDescription != null && !iv.getLastContentDescription().equals(contentDescription)) {
                nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                iv.setLastContentDescription(contentDescription.toString());
                return true;
            }
        }
        return false;
    }

}
