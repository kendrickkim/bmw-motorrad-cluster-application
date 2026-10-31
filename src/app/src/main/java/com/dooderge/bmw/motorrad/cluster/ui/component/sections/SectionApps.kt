package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp
import com.dooderge.bmw.motorrad.cluster.ui.theme.ClusterAppsIconColor

/**
 * 하단 Apps 슬롯 — Android Auto / Drawer / Setting / TPMS 단축 아이콘.
 * carStatus는 다른 섹션과 시그니처를 맞추기 위해 유지(현재 미사용).
 */
@Composable
fun SectionApps(
    @Suppress("UNUSED_PARAMETER") carStatus: CarStatus,
    alignment: Alignment = Alignment.TopStart,
    modifier: Modifier = Modifier,
    onApplicationEvent: (String) -> Unit = {},
) {
    SectionBox(
        modifier = modifier,
        alignment = alignment
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.designedDp()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBox("Android Auto", R.drawable.icon_app_android_auto) {
                    onApplicationEvent("android_auto")
                }
                AppBox("Drawer", R.drawable.icon_app_apps) {
                    onApplicationEvent("drawer")
                }
                AppBox("Setting", R.drawable.icon_app_setting) {
                    onApplicationEvent("setting")
                }
                AppBox("TPMS", R.drawable.icon_tire) {
                    onApplicationEvent("tpms")
                }
            }
        }
    }
}

/** Apps 슬롯 단일 아이콘 버튼. */
@Composable
private fun AppBox(
    description: String,
    resourceId: Int,
    onPress: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(150.designedDp(), 150.designedDp())
            .pointerInput(Unit) {
                detectTapGestures(onPress = { onPress() })
            },
        contentAlignment = Alignment.TopStart
    ) {
        Image(
            contentDescription = description,
            painter = painterResource(id = resourceId),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(ClusterAppsIconColor)
        )
    }
}
