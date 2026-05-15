package com.luntikius.wallet.wear.data

import com.luntikius.wallet.wearsync.WearPassSnapshot

data class CachedWearPass(val snapshot: WearPassSnapshot, val iconPath: String?, val logoPath: String?)
