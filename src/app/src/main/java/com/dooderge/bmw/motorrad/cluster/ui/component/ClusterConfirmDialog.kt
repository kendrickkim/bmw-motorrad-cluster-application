package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterActionButtonContainerColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterActionButtonContentColor

/**
 * 클러스터 Yes/No 확인 다이얼로그.
 * 테마 primary=Black 이라 TextButton 기본색이 안 보이므로 색을 명시한다.
 */
@Composable
fun ClusterConfirmDialog(
	title: String,
	message: String,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
	confirmLabel: String = "Yes",
	dismissLabel: String = "No",
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		properties = DialogProperties(usePlatformDefaultWidth = false),
		modifier = Modifier.widthIn(min = 360.dp, max = 520.dp),
		title = { Text(title, color = Color.White) },
		text = { Text(message, color = Color(0xFFCCCCCC)) },
		confirmButton = {
			Button(
				onClick = onConfirm,
				shape = RoundedCornerShape(10.dp),
				colors = ButtonDefaults.buttonColors(
					containerColor = ClusterActionButtonContainerColor,
					contentColor = ClusterActionButtonContentColor
				)
			) { Text(confirmLabel) }
		},
		dismissButton = {
			TextButton(
				onClick = onDismiss,
				shape = RoundedCornerShape(10.dp),
				colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
			) { Text(dismissLabel) }
		}
	)
}
