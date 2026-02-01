package com.dooderge.bmw.motorrad.cluster.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dooderge.bmw.motorrad.cluster.CarStatus

@Composable
fun SectionDistance(
    carStatus: CarStatus,
    alignment: Alignment = Alignment.TopStart,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier.border(width = 1.dp, color = androidx.compose.ui.graphics.Color.Red),
        contentAlignment = alignment
    )
    {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Box(
                modifier = Modifier
                    .height(10.dp)
                    .fillMaxWidth()
            )
            {
                Box(modifier = Modifier)
                {
                    Text(text = "${0} KM")
                }
            }
        }
    }
}

