package com.learning.attendancetracking.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.camera.core.ImageCaptureCapabilities
import androidx.documentfile.provider.DocumentFile
import com.learning.attendancetracking.database.entity.AttendanceEntity
import com.learning.attendancetracking.database.entity.UserAttendanceRelation
import timber.log.Timber
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

private const val REPORT_FILE_NAME = "attendance_report.csv"

fun formatDate(date: Date): String {
    val dateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    return DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss").format(dateTime)
}

@OptIn(ExperimentalImageCaptureOutputFormat::class)
fun logImageCaptureCapabilities(capabilities: ImageCaptureCapabilities) {
    Timber.d(
        "ImageCaptureCapabilities: CaptureProcessProgressSupported=${capabilities.isCaptureProcessProgressSupported}, PostViewSupported=${capabilities.isPostviewSupported}, SupportedOutputFormats=${
            capabilities.supportedOutputFormats.joinToString(
                separator = ","
            )
        }"
    )
}

fun exportAttendanceReport(
    context: Context,
    destDirFile: DocumentFile,
    recordsList: List<UserAttendanceRelation>
) {
    destDirFile.findFile(REPORT_FILE_NAME)?.delete()
    val destDocumentFile = destDirFile.createFile("text/csv", REPORT_FILE_NAME) ?: return
    context.contentResolver.openOutputStream(destDocumentFile.uri)?.use { outputStream ->
        val columns = listOf(
            "Employee ID", "Name", "Department", "Login time", "Logout time", "Location"
        ).joinToString(separator = ",")
        outputStream.write(columns.toByteArray())
        outputStream.write("\n".toByteArray())
        recordsList.forEach { record ->
            with(record) {
                attendanceHistory.forEach { attendanceEntity: AttendanceEntity ->
                    val line = listOf(
                        userEntity.userId,
                        userEntity.name,
                        userEntity.department,
                        formatDate(attendanceEntity.loginTime),
                        attendanceEntity.logoutTime?.let { formatDate(it) }.orEmpty(),
                        "${userEntity.latitude}:${userEntity.longitude}"
                    ).joinToString(separator = ",")
                    outputStream.write(line.toByteArray())
                    outputStream.write("\n".toByteArray())
                }
            }
        }
        outputStream.flush()
    }
    openReportFile(context, destDocumentFile.uri)
}

fun openReportFile(context: Context, uri: Uri) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }.onFailure {
        Timber.e(it, "No application found that can open CSV file")
    }
}

