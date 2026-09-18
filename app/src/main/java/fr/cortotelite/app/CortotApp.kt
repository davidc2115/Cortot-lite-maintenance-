package fr.cortotelite.app

import android.app.Application
import fr.cortotelite.app.data.AppDb
import fr.cortotelite.app.data.Repo

class CortotApp : Application() {
    val repo by lazy { Repo(AppDb.get(this).dao()) }
}
