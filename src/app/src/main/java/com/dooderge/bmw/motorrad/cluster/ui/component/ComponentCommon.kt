package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterAccentColor
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterValueColor

/** 값+단위를 서로 다른 색의 AnnotatedString으로 만든다. */
fun clusterValueUnitText(
	value: String,
	unit: String,
	valueColor: Color = ClusterValueColor,
	unitColor: Color = ClusterAccentColor,
): AnnotatedString = buildAnnotatedString {
	withStyle(SpanStyle(color = valueColor)) { append(value) }
	withStyle(SpanStyle(color = unitColor)) { append(unit) }
}

/** 클러스터 단일색 텍스트. fontSize는 디자인 기준값(실제 sp = /3). */
@Composable
fun ClusterText(
	text: String,
	fontSize: Float,
	fontWeight: FontWeight = FontWeight.Normal,
	color: Color = ClusterValueColor,
	softWrap: Boolean = true,
	maxLines: Int = Int.MAX_VALUE
) {
	val nfontSize = fontSize / 3f
	Text(
		text = text,
		fontSize = nfontSize.sp,
		fontWeight = fontWeight,
		color = color,
		softWrap = softWrap,
		maxLines = maxLines,
		style = clusterTextStyle()
	)
}

/** 값/단위 색이 섞인 AnnotatedString용 ClusterText. */
@Composable
fun ClusterText(
	text: AnnotatedString,
	fontSize: Float,
	fontWeight: FontWeight = FontWeight.Normal,
	softWrap: Boolean = true,
	maxLines: Int = Int.MAX_VALUE
) {
	val nfontSize = fontSize / 3f
	Text(
		text = text,
		fontSize = nfontSize.sp,
		fontWeight = fontWeight,
		softWrap = softWrap,
		maxLines = maxLines,
		style = clusterTextStyle()
	)
}

/** includeFontPadding 제거로 섹션 간 세로 정렬을 맞춘다. */
private fun clusterTextStyle() = TextStyle(
	platformStyle = PlatformTextStyle(
		includeFontPadding = false
	),
	lineHeightStyle = LineHeightStyle(
		alignment = LineHeightStyle.Alignment.Center,
		trim = LineHeightStyle.Trim.Both
	)
)
