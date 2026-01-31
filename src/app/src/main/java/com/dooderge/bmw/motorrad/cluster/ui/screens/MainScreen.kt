package com.dooderge.bmw.motorrad.cluster.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dooderge.bmw.motorrad.cluster.CarStatus
import com.dooderge.bmw.motorrad.cluster.R
import com.dooderge.bmw.motorrad.cluster.SettingPreference
import com.dooderge.bmw.motorrad.cluster.viewmodel.MainViewModel
import java.util.Timer
import java.util.TimerTask

@Composable
fun MainScreen(
	onPermissionsGranted: () -> Unit,
	viewModel: MainViewModel = viewModel(),
	onAndroidAutoClick: () -> Unit = {},
	onSettingsClick: () -> Unit = {},
	onLauncherClick: () -> Unit = {},
	context: Context,
	carStatus: CarStatus
) {

	var strMutTime by remember { mutableStateOf("00:00:00") }
	var strMutDate by remember { mutableStateOf("2026-01-01") }
	var strWifiName by remember { mutableStateOf("Unknown") }
	var strBtDeviceName by remember { mutableStateOf("NONE") }

	var sizeScreen by remember { mutableStateOf(IntSize(1, 1)) }
	var sizeBackgroundImage by remember { mutableStateOf(IntSize(1, 1)) }
	var sizeDisplayBackgroundImage by remember { mutableStateOf(IntSize(1, 1)) }
	var sizeRpmPinImage by remember { mutableStateOf(IntSize(1, 1)) }
	var sizeDisplayRpmPinImage by remember { mutableStateOf(IntSize(1, 1)) }


	val scaleImage = remember {
		mutableFloatStateOf(1f)
	}

	val rpmDegreeStart = -131.5f
	val rpmDegreeEnd = 131.5f

	var degreeRpmPin by remember { mutableFloatStateOf(rpmDegreeStart) }


	LaunchedEffect(Unit) {
//		val timerRefreshDateTime = Timer()
//
//		timerRefreshDateTime.schedule(object : TimerTask() {  // 시간 / 날짜 리프레시 타이머
//			override fun run() {
//				strMutTime = java.text.SimpleDateFormat(
//					"HH:mm:ss",
//					java.util.Locale.getDefault()
//				).format(java.util.Date())
//
//				strMutDate = java.text.SimpleDateFormat(
//					"yyyy-MM-dd",
//					java.util.Locale.getDefault()
//				).format(java.util.Date())
//
//				val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
//				val wifiInfo = wifiManager.connectionInfo
//				strWifiName = wifiInfo.ssid
//				val btDeviceName: String? = SettingPreference.getDeviceName(context)
//				if (btDeviceName == null || btDeviceName.isEmpty()) btDeviceName == "NONE"
//				strBtDeviceName = btDeviceName.toString()
//
////				carStatus.rpm.value = (carStatus.rpm.value + 1) % 12000
//
//			}
//		}, 0, 100)
	}

	LaunchedEffect(sizeScreen)
	{
		if (sizeScreen.width > 1) {

			if (sizeBackgroundImage.width == 1) {
				val options = android.graphics.BitmapFactory.Options().apply {
					inJustDecodeBounds = true
				}

				android.graphics.BitmapFactory.decodeResource(
					context.resources,
					R.drawable.cluster_background_black,
					options
				)

				sizeBackgroundImage = IntSize(options.outWidth, options.outHeight)

				android.graphics.BitmapFactory.decodeResource(
					context.resources,
					R.drawable.rpm_pin_area,
					options
				)

				sizeRpmPinImage = IntSize(options.outWidth, options.outHeight)
			}


			val ratioScreen = sizeScreen.width.toFloat() / sizeScreen.height.toFloat()
			val ratioBackgroundImage =
				sizeBackgroundImage.width.toFloat() / sizeBackgroundImage.height.toFloat()

			if (ratioScreen > ratioBackgroundImage) {
				sizeDisplayBackgroundImage =
					IntSize(
						(sizeScreen.width.toFloat() / ratioBackgroundImage).toInt(),
						sizeScreen.height
					)

			} else {
				sizeDisplayBackgroundImage =
					IntSize(
						sizeScreen.width,
						(sizeScreen.width.toFloat() / ratioBackgroundImage).toInt()
					)
			}

			scaleImage.floatValue =
				(sizeDisplayBackgroundImage.width.toFloat() / sizeBackgroundImage.width.toFloat()) * 0.88f

			sizeDisplayRpmPinImage = IntSize(
				(sizeRpmPinImage.width * scaleImage.floatValue).toInt(),
				(sizeRpmPinImage.height * scaleImage.floatValue).toInt()
			)
		}
	}

	CompositionLocalProvider(
		LocalDensity provides Density(
			density = LocalDensity.current.density,
			fontScale = 1f
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black)
				.onSizeChanged({
					sizeScreen = IntSize(it.width, it.height)

				}), verticalArrangement = Arrangement.Center,
			horizontalAlignment = Alignment.CenterHorizontally
		)
		{
			Box(    // 화면 전체 background
				modifier = Modifier
					.fillMaxSize()
					.drawBehind {
						// 화면의 가로, 세로 크기를 가져옵니다.
						val screenWidth = this.size.width
						val screenHeight = this.size.height

						// 타원의 반지름을 설정합니다. 가로/세로 비율에 따라 타원이 됩니다.
						// 가로 길이를 기준으로 반지름을 설정하고, 세로 길이는 화면 비율에 맞춥니다.
						val radiusX = screenWidth
						val radiusY = screenHeight

						// 방사형 그라데이션 브러시를 생성합니다.
						// Canvas 내부에서 사용할 때는 center와 radius를 drawOval 내부에서 처리하므로
						// 브러시 자체에는 지정할 필요가 없습니다.
						val radialBrush = Brush.radialGradient(
							colorStops = arrayOf(
								0.0f to Color(0xff383838),
								0.8f to Color(0xff0f0f0f),
								1.0f to Color.Black // 가장자리를 더 어둡게 처리
							),
							// 중심은 drawOval에서 지정
							center = this.center,
							// 반지름은 더 큰 값을 기준으로 설정하여 타원을 완전히 덮도록 합니다.
							radius = kotlin.comparisons.maxOf(radiusX, radiusY) * 0.5f
						)

						// 1. 검은색 배경을 먼저 전체에 그립니다.
						drawRect(color = Color.Black)

						// 2. 그 위에 타원형 그라데이션을 그립니다.
						drawOval(
							brush = radialBrush,
							// 타원의 위치와 크기를 지정합니다.
							topLeft = Offset(center.x - radiusX, center.y - radiusY),
							size = androidx.compose.ui.geometry.Size(radiusX * 2, radiusY * 2)
						)
					},
				contentAlignment = Alignment.Center
			)
			{
				Box(            // 그림 사이즈에 맞춘 box
					modifier = Modifier
						.size(
							sizeDisplayBackgroundImage.width.dp,
							sizeDisplayBackgroundImage.height.dp
						),
					contentAlignment = Alignment.Center
				)
				{
					Box(
						modifier = Modifier
							.fillMaxSize()
					)// 클러스터 디자인 background
					{
						Image(
							contentDescription = "Cluster Background",
							painter = painterResource(id = R.drawable.cluster_background_black),
							modifier = Modifier
								.fillMaxSize(),
						)

						ClusterSpeedRpm(
							width = sizeDisplayRpmPinImage.width,
							height = sizeDisplayRpmPinImage.height,
							carStatus = carStatus,
							scale = scaleImage.floatValue
						)
					}




//              Text(
//                text = "       ${wifi_name} / ${strMutBtDeviceName.value}",
//                fontSize = 15.sp,
//                fontWeight = FontWeight.Bold,
//              )
//              Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//              )
//              {
//                Text(
//                  text = "${strMutTime.value} ",
//                  fontSize = 28.sp,
//                  fontWeight = FontWeight.ExtraBold,
//                  fontFamily = FontFamily(Font(R.font.robot_mono))
//                )
//              }
//            }
//
//
//            Row(
//              modifier = Modifier
//                .fillMaxSize()
//                .background(Color.Black)
//                .padding(8.dp)
//            ) {
//              // 좌측 버튼 영역
//              Column(
//                modifier = Modifier
//                  .fillMaxHeight()
//                  .width(150.dp),
//                verticalArrangement = Arrangement.SpaceBetween,
//                horizontalAlignment = Alignment.CenterHorizontally
//              ) {
//
//
//                Spacer(modifier = Modifier.height(8.dp))
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                  Box(
//                    modifier = Modifier
//                      .size(128.dp, 128.dp)
//                      .clip(RoundedCornerShape(15.dp))
//                      .background(Color.DarkGray)
//                      .clickable {
//                        onAndroidAutoClick()
//                      }
//
//                  )
//                  {
//                    Image(
//                      contentDescription = "Android auto",
//                      painter = painterResource(id = R.drawable.icon_android_auto),
//                      modifier = Modifier
//                        .fillMaxSize()
//                        .padding(20.dp),
//                      contentScale = ContentScale.Fit
//                    )
//                  }
////                        Spacer(modifier = Modifier.height(16.dp))
////                        Box(
////                            modifier = Modifier
////                                .size(128.dp, 128.dp)
////                                .clip(RoundedCornerShape(15.dp))
////                                .background(Color.DarkGray)
////                                .clickable {
////                                    onLauncherClick()
////                                }
////
////                        )
////                        {
////                            Image(
////                                contentDescription = "Apps",
////                                painter = painterResource(id = R.drawable.ic_main),
////                                modifier = Modifier
////                                    .fillMaxSize()
////                                    .padding(20.dp),
////                                contentScale = ContentScale.Fit
////                            )
////                        }
//                }
//                Spacer(modifier = Modifier.height(16.dp))
//                ComponentGearIndicator(carStatus.gear.value, onLongClick = {
//                  CoroutineScope(Dispatchers.Main).launch {
//                    val wifiSettingsIntent =
//                      Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
//                    wifiSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    context.startActivity(wifiSettingsIntent)
//
//                  }
//                })
//                Spacer(modifier = Modifier.height(8.dp))
//              }
//              Spacer(modifier = Modifier.width(16.dp))
//              Box(
//                modifier = Modifier
//                  .fillMaxSize()
//                  .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 0.dp),
//                contentAlignment = Alignment.BottomCenter
//              ) {
//                ComponentCluster(context = context, carStatus = carStatus)
//              }
//            }
				}
			}
		}

		Box(
			modifier = Modifier.fillMaxSize(),
			contentAlignment = Alignment.BottomEnd
		)
		{
			Box(
				modifier = Modifier
					.size(60.dp, 60.dp)
					.padding(10.dp)
					.pointerInput(Unit) {
						detectTapGestures(
							onPress = {
								onSettingsClick()
							}
						)
					})
			{
				Image(
					modifier = Modifier.fillMaxSize(),
					painter = painterResource(
						id = R.drawable.icon_setting
					),
					contentDescription = "icon_setting",

					)
			}
		}
	}
}

@Composable
fun ClusterSpeedRpm(
	width : Int,
	height : Int,
	scale : Float,
	carStatus: CarStatus,
//	modifier: Modifier = Modifier
)
{

	var rpm by remember { mutableStateOf(1) }

	LaunchedEffect(Unit) {
		val tmrRpmChangeTest = Timer()

		tmrRpmChangeTest.schedule(object : TimerTask() {  // 시간 / 날짜 리프레시 타이머
			override fun run() {
				carStatus.rpm.value = (carStatus.rpm.value + 1) % 12000
			}
		}, 0, 100)
	}

	val top_padding = (300.toFloat() * scale).dp

	Column(
		modifier = Modifier
			.fillMaxSize(),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,

		) {
		Box(
			modifier = Modifier.fillMaxSize(),

			){
			Text("${carStatus.rpm.value}")
		}
		Spacer(Modifier.height(top_padding))
		Box(
			modifier = Modifier.size(
				width.dp,
				height.dp
			)
		)
		{


			Image(
				contentDescription = "RPM pin",
				painter = painterResource(id = R.drawable.rpm_pin_area),
				modifier = Modifier
					.size(
						width.dp,
						height.dp
					)
					.rotate(0f),
			)
		}

	}
}

@Composable
fun Speedometer(
	speed: Int,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Text(
			text = speed.toString(),
			color = Color.White,
			style = TextStyle(
				fontSize = 120.sp,
				fontWeight = FontWeight.Bold
			)
		)
		Text(
			text = "km/h",
			color = Color.White,
			style = MaterialTheme.typography.titleLarge,
			modifier = Modifier.padding(top = 100.dp)
		)
	}
}

@Composable
fun RpmGauge(
	rpm: Int,
	modifier: Modifier = Modifier
) {
	Canvas(modifier = modifier) {
		val center = Offset(size.width / 2, size.height / 2)
		val radius = size.width.coerceAtMost(size.height) * 0.4f

		// RPM 게이지 배경
		drawArc(
			color = Color.DarkGray,
			startAngle = 180f,
			sweepAngle = 180f,
			useCenter = false,
			style = Stroke(width = 20f),
			topLeft = Offset(center.x - radius, center.y - radius),
			size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
		)

		// RPM 게이지 표시
		val rpmPercentage = (rpm / 10000f).coerceIn(0f, 1f)
		drawArc(
			color = when {
				rpmPercentage > 0.9f -> Color.Red
				rpmPercentage > 0.7f -> Color.Yellow
				else -> Color.Green
			},
			startAngle = 180f,
			sweepAngle = 180f * rpmPercentage,
			useCenter = false,
			style = Stroke(width = 20f),
			topLeft = Offset(center.x - radius, center.y - radius),
			size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
		)

		// RPM 값 표시
		drawContext.canvas.nativeCanvas.apply {
			drawText(
				rpm.toString(),
				center.x,
				center.y + 20f,
				android.graphics.Paint().apply {
					color = android.graphics.Color.WHITE
					textSize = 40f
					textAlign = android.graphics.Paint.Align.CENTER
				}
			)
		}
	}
}

@Composable
fun FuelGauge(
	fuelLevel: Float,
	modifier: Modifier = Modifier
) {
	Box(
		modifier = modifier,
		contentAlignment = Alignment.Center
	) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			val center = Offset(size.width / 2, size.height / 2)
			val radius = size.width.coerceAtMost(size.height) * 0.4f

			// 연료 게이지 배경
			drawArc(
				color = Color.DarkGray,
				startAngle = -90f,
				sweepAngle = 180f,
				useCenter = false,
				style = Stroke(width = 8f),
				topLeft = Offset(center.x - radius, center.y - radius),
				size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
			)

			// 연료량 표시
			drawArc(
				color = when {
					fuelLevel < 0.2f -> Color.Red
					fuelLevel < 0.4f -> Color.Yellow
					else -> Color.Green
				},
				startAngle = -90f,
				sweepAngle = 180f * fuelLevel,
				useCenter = false,
				style = Stroke(width = 8f),
				topLeft = Offset(center.x - radius, center.y - radius),
				size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
			)
		}
	}
}
