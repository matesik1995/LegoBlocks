package com.matesik.legoblocks

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_inventory.*
import kotlinx.android.synthetic.main.content_inventory.*
import kotlinx.android.synthetic.main.dialog_save_xml.view.*
import kotlinx.android.synthetic.main.parts_list_item.view.*
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class InventoryActivity : AppCompatActivity() {
    lateinit var databaseHelper: DataBaseHelper
    lateinit var project: Project
    lateinit var partListAdapter: PartListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventory)
        databaseHelper = DataBaseHelper(this)
        val id = intent.getIntExtra("id", 0)
        project = databaseHelper.getProject(id)!!
        supportActionBar?.title = project.name

        val parts = databaseHelper.fetchInventoryParts(id)
        partListAdapter = PartListAdapter(this, ArrayList(parts), databaseHelper)
        partsList.adapter = partListAdapter
    }

    fun saveXMLDialog(view: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Save as XML")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_xml, null)
        dialogView.fileNameEditText.setText(project.name)
        builder.setView(dialogView)
        builder.setPositiveButton("Save", { dialog, which ->
            run {
                val fileNameInput = dialogView.fileNameEditText.text.toString()
                val fileName = if (!fileNameInput.endsWith(".xml")) {
                    fileNameInput + ".xml"
                } else {
                    fileNameInput
                }
                writeXML(fileName)
            }
        })
        builder.setNegativeButton("Cancel", { dialog, which -> dialog.cancel() })

        builder.show()
    }

    private fun writeXML(fileName: String) {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val root = doc.createElement("INVENTORY")
        for (i in 0 until partListAdapter.count) {
            val part = partListAdapter.getItem(i)
            if (part.qtyInStore != part.qtyInSet) {
                val item = doc.createElement("ITEM")

                val itemType = doc.createElement("ITEMTYPE")
                itemType.appendChild(doc.createTextNode(databaseHelper.getTypeCodeById(part.typeId)))
                item.appendChild(itemType)

                val itemId = doc.createElement("ITEMID")
                itemId.appendChild(doc.createTextNode(part.itemId.toString()))
                item.appendChild(itemId)

                val color = doc.createElement("COLOR")
                color.appendChild(doc.createTextNode(part.colorId.toString()))
                item.appendChild(color)

                val qty = doc.createElement("QTYFILLED")
                qty.appendChild(doc.createTextNode((part.qtyInSet - part.qtyInStore).toString()))
                item.appendChild(qty)

                root.appendChild(item)
            }
        }
        doc.appendChild(root)

        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        try {
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName)
            transformer.transform(DOMSource(doc), StreamResult(file))
            Snackbar.make(inventoryActivity, "File $fileName saved", Snackbar.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("FILE ERROR", e.message, e)
            Snackbar.make(inventoryActivity, "Something went wrong", Snackbar.LENGTH_LONG).show()
        }

    }
}

class PartListAdapter(context: Context, list: ArrayList<InventoryPart>, val databaseHelper: DataBaseHelper) : ArrayAdapter<InventoryPart>(context, 0, list) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
                ?: LayoutInflater.from(context).inflate(R.layout.parts_list_item, parent, false)
        val part = getItem(position)
        view.partName.text = databaseHelper.getNameByItemId(part.itemId)
        view.partId.text = "ID: ${part.itemId}"
        view.quantity.text = "Quantity: ${part.qtyInStore}/${part.qtyInSet}"
        databaseHelper.getImage(part.itemId, part.colorId)?.let {
            view.thumbnail.setImageBitmap(it)
        }
        view.increaseQty.setOnClickListener({
            if (part.qtyInStore < part.qtyInSet) {
                databaseHelper.increaseQty(part)
                remove(part)
                insert(part.copy(qtyInStore = part.qtyInStore + 1), position)
                notifyDataSetChanged()
            }
        })
        view.decreaseQty.setOnClickListener({
            if (part.qtyInStore > 0) {
                databaseHelper.decreaseQty(part)
                remove(part)
                insert(part.copy(qtyInStore = part.qtyInStore - 1), position)
                notifyDataSetChanged()
            }
        })

        if (part.qtyInStore == part.qtyInSet) {
            view.setBackgroundColor(Color.argb(125, 128, 255, 128))
        } else {
            view.setBackgroundColor(Color.argb(0, 128, 255, 128))
        }
        return view
    }
}