package com.sf.riderhelper;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class ScreenInteractor {
    private final AccessibilityService service;

    public ScreenInteractor(AccessibilityService s) {
        this.service = s;
    }

    // 按文本查找节点
    public AccessibilityNodeInfo findNodeByText(String text) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        AccessibilityNodeInfo result = nodes.isEmpty() ? null : nodes.get(0);
        root.recycle();
        return result;
    }

    // 按ID查找节点
    public AccessibilityNodeInfo findNodeById(String id) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
        AccessibilityNodeInfo result = nodes.isEmpty() ? null : nodes.get(0);
        root.recycle();
        return result;
    }

    // 按内容描述查找
    public AccessibilityNodeInfo findNodeByDesc(String desc) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return null;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(desc);
        for (AccessibilityNodeInfo n : nodes) {
            if (desc.equals(n.getContentDescription())) {
                root.recycle();
                return n;
            }
        }
        root.recycle();
        return null;
    }

    // 点击节点
    public boolean clickNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        boolean ok = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return ok;
    }

    // 查找并点击文本
    public boolean clickByText(String text) {
        AccessibilityNodeInfo n = findNodeByText(text);
        return n != null && (n.performAction(AccessibilityNodeInfo.ACTION_CLICK) || true);
    }

    // 获取页面文本内容（用于识别订单）
    public String getPageText() {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return "";
        String text = nodeToText(root);
        root.recycle();
        return text;
    }

    private String nodeToText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();
        if (node.getText() != null) sb.append(node.getText()).append("\n");
        for (int i = 0; i < node.getChildCount(); i++) {
            sb.append(nodeToText(node.getChild(i)));
        }
        return sb.toString();
    }

    // 查找第一个包含关键字的节点
    public AccessibilityNodeInfo findNodeContaining(String keyword) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return null;
        AccessibilityNodeInfo result = findInSubtree(root, keyword);
        root.recycle();
        return result;
    }

    private AccessibilityNodeInfo findInSubtree(AccessibilityNodeInfo node, String keyword) {
        if (node == null) return null;
        CharSequence t = node.getText();
        if (t != null && t.toString().contains(keyword)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findInSubtree(child, keyword);
            if (found != null) return found;
            if (child != null) child.recycle();
        }
        return null;
    }
}
