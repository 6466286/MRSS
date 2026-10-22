package com.tgwgroup.MiRearScreenSwitcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only exports numeric geometry and known window modes, never window titles or intents. */
final class RearLayoutDiagnostics {
    private static final Pattern GEOMETRY = Pattern.compile(
        "\\b(mFrame|frame|parentFrame|displayFrame|compatFrame|mBounds|bounds|mAppBounds|appBounds|"
        + "mParentFrame|mDisplayFrame|mCompatFrame|mRequestedWidth|mRequestedHeight|mDisplayId|displayId|"
        + "mGlobalScale|mInvGlobalScale|mCompatScale|mSurfacePosition|mWindowingMode|windowingMode|"
        + "mRotation|rotation|taskId|rootTaskId)=(Rect\\([0-9 ,.-]+\\)|Point\\([0-9 ,.-]+\\)|"
        + "\\[[0-9 ,.-]+\\](?:\\[[0-9 ,.-]+\\])?|\\([0-9 ,.-]+\\)|-?[0-9]+(?:\\.[0-9]+)?|fullscreen|freeform)");

    static String filter(String input, boolean windows) {
        StringBuilder out = new StringBuilder();
        boolean bili = false;
        String root = "";
        int count = 0;
        for (String line : input.split("\\n")) {
            String trimmed = line.trim();
            if (windows) {
                if (trimmed.matches("Window #.*") || trimmed.matches("Window\\{.*")) {
                    bili = trimmed.contains("tv.danmaku.bili/");
                    if (bili) out.append("Bili window ").append(++count).append('\n');
                }
            } else {
                if (trimmed.startsWith("RootTask")) root = values(trimmed);
                bili = trimmed.contains("tv.danmaku.bili/") && trimmed.contains("taskId=");
                if (bili) out.append("Bili task ").append(++count).append(" root: ").append(root).append('\n');
            }
            if (bili) {
                String value = values(trimmed);
                if (!value.isEmpty()) out.append(value).append('\n');
            }
            if (out.length() > 10000) break;
        }
        return out.length() == 0 ? "No readable Bili geometry\n" : out.toString();
    }
    private static String values(String line) {
        Matcher matcher = GEOMETRY.matcher(line);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) out.append(matcher.group()).append(' ');
        return out.toString().trim();
    }
}
