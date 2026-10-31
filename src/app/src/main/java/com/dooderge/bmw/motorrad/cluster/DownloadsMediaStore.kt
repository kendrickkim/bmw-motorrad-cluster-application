package com.dooderge.bmw.motorrad.cluster

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/**
 * Downloads MediaStore 조회 공통 유틸.
 * RELATIVE_PATH 끝 슬래시 유무 차이를 흡수한다.
 */
object DownloadsMediaStore {
	/**
	 * 동일 DISPLAY_NAME + RELATIVE_PATH 의 기존 항목 URI를 찾는다.
	 * 없으면 null.
	 */
	fun findByDisplayName(
		context: Context,
		displayName: String,
		relativeDir: String,
	): Uri? {
		val projection = arrayOf(MediaStore.Downloads._ID)
		val selection =
			"${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
		queryFirstId(context, projection, selection, arrayOf(displayName, "$relativeDir/"))
			?.let { return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, it) }
		// RELATIVE_PATH trailing slash 차이 대비
		queryFirstId(context, projection, selection, arrayOf(displayName, relativeDir))
			?.let { return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, it) }
		return null
	}

	/** selection/args에 맞는 첫 행의 _ID. 없으면 null. */
	private fun queryFirstId(
		context: Context,
		projection: Array<String>,
		selection: String,
		args: Array<String>,
	): Long? {
		context.contentResolver.query(
			MediaStore.Downloads.EXTERNAL_CONTENT_URI,
			projection,
			selection,
			args,
			null
		)?.use { cursor ->
			if (cursor.moveToFirst()) return cursor.getLong(0)
		}
		return null
	}
}
