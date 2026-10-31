package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterSideButtonContainerColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterSideButtonContentColor

/** 설정 / TPMS / 앱 리스트 / 로그 오른쪽 액션 열 공통 너비(Row weight) */
const val ClusterSidePanelWeight = 0.22f

/**
 * 왼쪽 콘텐츠 + 오른쪽 액션 버튼 공통 골격.
 * 설정 / TPMS / 앱 리스트 / 로그 화면에서 사용.
 */
@Composable
fun ClusterTwoPaneScaffold(
	modifier: Modifier = Modifier,
	title: String? = null,
	subtitle: String? = null,
	content: @Composable BoxScope.() -> Unit,
	sideActions: @Composable ColumnScope.() -> Unit,
) {
	Column(
		modifier = modifier
			.fillMaxSize()
			.padding(20.dp)
	) {
		if (title != null) {
			Text(
				text = title,
				fontSize = 22.sp,
				fontWeight = FontWeight.Bold,
				color = Color.White
			)
		}
		if (subtitle != null) {
			Text(
				text = subtitle,
				fontSize = 13.sp,
				color = Color(0xFFAAAAAA),
				modifier = Modifier.padding(top = if (title != null) 4.dp else 0.dp)
			)
		}
		if (title != null || subtitle != null) {
			Spacer(modifier = Modifier.height(12.dp))
		}
		Row(
			modifier = Modifier
				.weight(1f)
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			Box(
				modifier = Modifier
					.weight(1f)
					.fillMaxHeight(),
				content = content
			)
			ClusterSideActionColumn(
				modifier = Modifier.weight(ClusterSidePanelWeight),
				content = sideActions
			)
		}
	}
}

@Composable
fun ClusterSideActionColumn(
	modifier: Modifier = Modifier,
	content: @Composable ColumnScope.() -> Unit,
) {
	Column(
		modifier = modifier.fillMaxHeight(),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		content = content
	)
}

@Composable
fun ClusterSideActionButton(
	label: String,
	modifier: Modifier = Modifier,
	fontSize: TextUnit = 18.sp,
	enabled: Boolean = true,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
		enabled = enabled,
		modifier = modifier.fillMaxSize(),
		shape = RoundedCornerShape(10.dp),
		colors = ButtonDefaults.buttonColors(
			containerColor = ClusterSideButtonContainerColor,
			contentColor = ClusterSideButtonContentColor,
			disabledContainerColor = ClusterSideButtonContainerColor.copy(alpha = 0.35f),
			disabledContentColor = ClusterSideButtonContentColor.copy(alpha = 0.45f)
		)
	) {
		Text(
			text = label,
			fontSize = fontSize,
			fontWeight = FontWeight.ExtraBold,
			textAlign = TextAlign.Center
		)
	}
}
