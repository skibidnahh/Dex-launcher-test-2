package com.example.dexlauncher;

interface IShizukuService {
    int executeCommand(String command);
    String executeCommandWithOutput(String command);
    void launchWindow(String packageName, int left, int top, int right, int bottom);
    void resizeTask(int taskId, int left, int top, int right, int bottom);
    void setTaskWindowingMode(int taskId, int windowingMode);
    void closeTask(int taskId);
    String getRecentTasks();
    void destroy();
}
