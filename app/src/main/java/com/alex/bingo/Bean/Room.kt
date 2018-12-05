package com.alex.bingo.Bean


data class Room (
    var id: String ?="",
    var title: String ?="",
    var init: Member ?=null,
    var join: Member ?=null,
    var status: Int = 0)
{
    constructor(title:String?,init: Member?) : this() {
        this.title=title
        this.init=init
    }

    companion object {
        const val STATUS_INIT = 0
        const val STATUS_CREATED = 1
        const val STATUS_JOINED = 2
        const val STATUS_CREATOR_TURN = 3
        const val STATUS_JOINERS_TURN = 4
        const val STATUS_CREATOR_BINGO = 5
        const val STATUS_JOINERS_BINGO = 6
    }
}
