package com.tgwgroup.MiRearScreenSwitcher;

public class RearLayoutDiagnosticsTest {
    public static void main(String[] args) {
        String dump = "Window #0 Window{123 u0 tv.danmaku.bili/.MainActivity secret title}:\n"
            + " mDisplayId=1 mGlobalScale=1.0 mFrame=[0,0][1033,972]\n"
            + " mSurfacePosition=Point(0, 0) mAttrs=account-token\n"
            + "Window #1 Window{456 u0 other.app/.Activity}:\n"
            + " mDisplayId=0 mFrame=[1,2][3,4] password=secret\n";
        String filtered = RearLayoutDiagnostics.filter(dump, true);
        if (!filtered.contains("mFrame=[0,0][1033,972]") || !filtered.contains("Point(0, 0)"))
            throw new AssertionError("Missing geometry");
        if (filtered.contains("secret") || filtered.contains("account") || filtered.contains("[1,2]"))
            throw new AssertionError("Unrelated data leaked");
        String stack = "RootTask id=8 bounds=[0,0][1536,972] displayId=1\n"
            + " taskId=42: tv.danmaku.bili/.Main bounds=[503,0][1536,972] search=private\n"
            + " taskId=43: other.app/.Main bounds=[0,0][2,3]\n";
        String tasks = RearLayoutDiagnostics.filter(stack, false);
        if (!tasks.contains("taskId=42") || !tasks.contains("displayId=1") || tasks.contains("private")
            || tasks.contains("taskId=43")) throw new AssertionError("Incorrect task filtering");
        if (!RearLayoutDiagnostics.filter("", true).contains("No readable"))
            throw new AssertionError("Empty result not explicit");
        System.out.println("PASS: window/task geometry extraction and privacy filtering");
    }
}
