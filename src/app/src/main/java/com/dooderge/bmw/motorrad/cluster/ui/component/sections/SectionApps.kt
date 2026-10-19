package com.dooderge.bmw.motorrad.cluster.ui.component.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.ui.component.ClusterText
import com.dooderge.bmw.motorrad.cluster.ui.component.designedDp

@Composable
fun SectionApps(
    carStatus: CarStatus,
    alignment: Alignment = Alignment.TopStart,
    modifier: Modifier = Modifier,
    onApplicationEvent: (String) -> Unit = {},
) {

    @Composable
    fun AppBox(description: String, resouceId: Int, onPress: () -> Unit = {}) {
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = LocalDensity.current.density,
                fontScale = 1f
            )
        ) {
            Box(
                modifier = Modifier
                    .size(150.designedDp(), 150.designedDp())
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onPress()
                            }
                        )
                    },
                contentAlignment = Alignment.TopStart
            )
            {
                Image(
                    contentDescription = description,
                    painter = painterResource(id = resouceId),
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(0xffaaaaaa))
                )
            }

        }
    }

    SectionBox(
        modifier = modifier,
        alignment = alignment
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.designedDp()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        )
        {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Absolute.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            )
            {
                AppBox("Android Auto", R.drawable.icon_app_android_auto, onPress = {
                    onApplicationEvent("android_auto")
                })
                AppBox("Drawer", R.drawable.icon_app_apps, onPress = {
                    onApplicationEvent("drawer")
                })
                AppBox("Setting", R.drawable.icon_app_setting, onPress = {
                    onApplicationEvent("setting")
                })

            }
        }
    }
}

