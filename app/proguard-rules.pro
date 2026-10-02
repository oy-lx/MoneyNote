# Room 生成代码在 release 混淆下需要的保留规则（当前 release 未开启混淆，仅作预留）
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**
