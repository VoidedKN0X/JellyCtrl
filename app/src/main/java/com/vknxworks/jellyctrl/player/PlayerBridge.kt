package com.vknxworks.jellyctrl.player

/**
 * The video player that is open right now, if any, so that commands from outside the player screen (another
 * device controlling this phone, a SyncPlay group) reach it. Set and cleared on the main thread by
 * [PlayerViewModel].
 */
object PlayerBridge {
    @Volatile var active: PlayerViewModel? = null
}
