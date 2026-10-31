package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** 클러스터 섹션 공통 컨테이너 — alignment만 감싼 Box. */
@Composable
fun SectionBox(
	modifier: Modifier = Modifier,
	alignment: Alignment = Alignment.TopStart,
	content: @Composable BoxScope.() -> Unit
) {
	Box(
		modifier = modifier,
		contentAlignment = alignment,
		content = content
	)
}
