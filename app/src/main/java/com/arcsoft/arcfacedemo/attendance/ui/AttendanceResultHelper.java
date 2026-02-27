package com.arcsoft.arcfacedemo.attendance.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcsoft.arcfacedemo.R;
import com.arcsoft.arcfacedemo.attendance.model.CheckResult;

/**
 * 打卡结果显示辅助类
 */
public class AttendanceResultHelper {

    public static void showResult(Context context, CheckResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.TechTheme);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_attendance_result, null);

        ImageView statusIcon = view.findViewById(R.id.status_icon);
        TextView statusTitle = view.findViewById(R.id.status_title);
        TextView statusMessage = view.findViewById(R.id.status_message);
        TextView statusTime = view.findViewById(R.id.status_time);

        if (result.isSuccess()) {
            // 成功
            statusIcon.setImageResource(android.R.drawable.checkbox_on_background);
            statusIcon.setColorFilter(Color.parseColor("#00FF00"));

            String title;
            switch (result.getStatus()) {
                case "NORMAL":
                    title = "打卡成功";
                    statusTitle.setTextColor(Color.parseColor("#00FF00"));
                    break;
                case "LATE":
                    title = "打卡成功 - 迟到";
                    statusTitle.setTextColor(Color.parseColor("#FFCC00"));
                    break;
                case "EARLY":
                    title = "打卡成功 - 早退";
                    statusTitle.setTextColor(Color.parseColor("#FFCC00"));
                    break;
                default:
                    title = "打卡成功";
                    statusTitle.setTextColor(Color.parseColor("#00FFFF"));
            }

            statusTitle.setText(title);
            statusMessage.setText(result.getMessage());
        } else {
            // 失败
            statusIcon.setImageResource(android.R.drawable.ic_delete);
            statusIcon.setColorFilter(Color.parseColor("#FF3333"));
            statusTitle.setText("打卡失败");
            statusTitle.setTextColor(Color.parseColor("#FF3333"));
            statusMessage.setText(result.getError());
        }

        // 显示时间
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm:ss");
        statusTime.setText("时间: " + dateFormat.format(new java.util.Date(result.getCheckTime())));

        builder.setView(view);
        builder.setPositiveButton("确定", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void showCheckInSuccess(Context context, String employeeName, String status) {
        String message = "员工: " + employeeName + "\n" + getStatusText(status);
        CheckResult result = CheckResult.success("CHECK_IN", status, message);
        showResult(context, result);
    }

    public static void showCheckOutSuccess(Context context, String employeeName, String status) {
        String message = "员工: " + employeeName + "\n" + getStatusText(status);
        CheckResult result = CheckResult.success("CHECK_OUT", status, message);
        showResult(context, result);
    }

    public static void showError(Context context, String error) {
        CheckResult result = CheckResult.failed(error);
        showResult(context, result);
    }

    private static String getStatusText(String status) {
        switch (status) {
            case "NORMAL":
                return "正常";
            case "LATE":
                return "迟到";
            case "EARLY":
                return "早退";
            case "ABSENT":
                return "缺勤";
            default:
                return status;
        }
    }
}
