package com.matesik.legoblocks

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.dialog_new_project.view.*
import kotlinx.android.synthetic.main.project_list_item.view.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    lateinit var projectListAdapter: ProjectListAdapter
    lateinit var myDbHelper: DataBaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        requestPermissions(arrayOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"), 200)

        myDbHelper = DataBaseHelper(this)
//        myDbHelper.deleteDB()
        myDbHelper.createDataBase()

        val projects = myDbHelper.fetchProjects()
        projectListAdapter = ProjectListAdapter(this, ArrayList(projects), myDbHelper)
        projectList.adapter = projectListAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun newProjectDialog(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("New project")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_project, null)
        builder.setView(dialogView)
        builder.setPositiveButton("Create", { dialog, which ->
            run {
                val project = Project(
                        id = Integer.parseInt(dialogView.inventoryIdEditText.text.toString()),
                        name = dialogView.projectNameEditText.text.toString(),
                        active = 1,
                        lastAccessed = (System.currentTimeMillis() / 1000).toInt()
                )
                if (myDbHelper.getProject(project.id) != null) {
                    Snackbar.make(view, "Project with inventory id ${project.id} already exists", Snackbar.LENGTH_LONG).show()
                } else {
                    CreateProjectWithInventory(project).execute(project.id)
                }
            }
        })
        builder.setNegativeButton("Abort", { dialog, which -> dialog.cancel() })

        builder.show()
    }

    private inner class CreateProjectWithInventory(val project: Project) : AsyncTask<Int, Unit, List<InventoryPart>>() {
        val apiUrl = getSharedPreferences("myPreferences", Context.MODE_PRIVATE).getString("apiUrl", "http://fcds.cs.put.poznan.pl/MyWeb/BL/")
        val ext = ".xml"

        override fun onPreExecute() {
            super.onPreExecute()
            progressBar.visibility = View.VISIBLE
            projectList.visibility = View.GONE
        }

        override fun doInBackground(vararg ids: Int?): List<InventoryPart> {
//            Thread.sleep(1000)
            val inventoryId = ids[0]!!
            val parts = LinkedList<InventoryPart>()
            try {
                val url = URL(apiUrl + inventoryId + ext)
                val urlConnection = url.openConnection() as HttpURLConnection
                try {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val xpp = factory.newPullParser()

                    xpp.setInput(InputStreamReader(urlConnection.inputStream))

                    var itemType: String = ""
                    var itemId: String = ""
                    var qty: Int = 0
                    var color: Int = 0
                    while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                        when (xpp.eventType) {
                            XmlPullParser.START_TAG -> {
                                when (xpp.name) {
                                    "ITEMTYPE" -> {
                                        if (xpp.next() == XmlPullParser.TEXT) {
                                            itemType = xpp.text
                                        }
                                    }
                                    "ITEMID" -> {
                                        if (xpp.next() == XmlPullParser.TEXT) {
                                            itemId = xpp.text
                                        }
                                    }
                                    "QTY" -> {
                                        if (xpp.next() == XmlPullParser.TEXT) {
                                            qty = Integer.parseInt(xpp.text)
                                        }
                                    }
                                    "COLOR" -> {
                                        if (xpp.next() == XmlPullParser.TEXT) {
                                            color = Integer.parseInt(xpp.text)
                                        }
                                    }
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                if (xpp.name == "ITEM") {
                                    try {
                                        parts.add(InventoryPart(
                                                inventoryId = inventoryId,
                                                typeId = myDbHelper.getTypeIdByCode(itemType),
                                                itemId = myDbHelper.getItemIdByCode(itemId),
                                                qtyInSet = qty,
                                                qtyInStore = 0,
                                                colorId = color,
                                                extra = 0
                                        ))
                                    } catch (e: Exception) {
                                        Log.e("ERROR", e.message, e)
                                    }
                                }
                            }
                        }
                        xpp.next()
                    }
                } finally {
                    urlConnection.disconnect()
                }
            } catch (e: Exception) {
                Log.e("ERROR", e.message, e)
            }
            Log.i("Info", "here")
            parts.forEach({
                var bitmap: Bitmap? = null
                val imageCode = myDbHelper.getImageCode(it.itemId, it.colorId)
                if (imageCode != null) {
                    Log.i("Info", "here")
                    bitmap = getBitmapFromURL("https://www.lego.com/service/bricks/5/2/" + imageCode)
                }
                if (bitmap == null) {
                    val itemCode = myDbHelper.getCodeByItemId(it.itemId)
                    bitmap = getBitmapFromURL("http://img.bricklink.com/P/" + it.colorId + "/" + itemCode + ".gif")
                }
                if (bitmap != null) {
                    myDbHelper.insertImage(it.itemId, it.colorId, bitmap)
                }
            })
            return parts
        }

        override fun onPostExecute(result: List<InventoryPart>) {
            if (result.isEmpty()) {
                Snackbar.make(mainActivity, "Could not fetch inventory with id ${project.id}", Snackbar.LENGTH_LONG).show()
            } else {
                myDbHelper.insertInventoryParts(result)
                projectListAdapter.add(project)
                Snackbar.make(mainActivity, "Project ${project.name} (${project.id}) successfully created", Snackbar.LENGTH_LONG).show()
                super.onPostExecute(result)
            }
            progressBar.visibility = View.GONE
            projectList.visibility = View.VISIBLE
        }

        private fun getBitmapFromURL(src: String): Bitmap? {
            var bitmap: Bitmap? = null
            try {
                val url = URL(src)
                bitmap = BitmapFactory.decodeStream(url.content as InputStream)
            } catch (e: IOException) {
                Log.e("ERROR", e.message)
            }
            return bitmap
        }
    }
}

class ProjectListAdapter(context: Context, list: ArrayList<Project>, val databaseHelper: DataBaseHelper) : ArrayAdapter<Project>(context, 0, list) {
    override fun add(project: Project) {
        databaseHelper.insertProject(project)
        super.add(project)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.project_list_item, parent, false)
        val project = getItem(position)
        view.projectName.text = project.name
        view.inventoryId.text = project.id.toString()
        view.lastAccess.text = SimpleDateFormat.getDateTimeInstance().format(project.lastAccessed.toLong() * 1000)
        view.setOnClickListener {
            val i = Intent(context, InventoryActivity::class.java).apply {
                putExtra("id", project.id)
            }
            val updated = project.copy(lastAccessed = (System.currentTimeMillis() / 1000).toInt())
            databaseHelper.updateProject(updated)
            remove(project)
            insert(updated, position)
            notifyDataSetChanged()
            startActivity(context, i, Bundle.EMPTY)
        }
        return view
    }
}