package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

@Composable
fun ClusterText(
	text: String,
	fontSize: Float,
	fontWeight: FontWeight = FontWeight.Normal,
	color: Color = Color(0xffaaaaaa)
) {
    var nfontSize = fontSize / 3;
	Text(
		text = text,
		fontSize = nfontSize.sp,
		fontWeight = fontWeight,
		color = color,
		style = TextStyle(
			// 폰트의 기본 상하단 여백을 제거합니다.
			platformStyle = PlatformTextStyle(
				includeFontPadding = false
			),
			// 추가적으로 줄 높이 조절이 필요할 경우 사용합니다.
			lineHeightStyle = LineHeightStyle(
				alignment = LineHeightStyle.Alignment.Center,
				trim = LineHeightStyle.Trim.Both
			)
		)
	)
}

