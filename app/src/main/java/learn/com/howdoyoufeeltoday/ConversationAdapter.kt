package learn.com.howdoyoufeeltoday

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.*

/**
 * Created by Administrator on 6/4/2017.
 */
class ConversationAdapter(var context:Context,var list:ArrayList<Conversation>) : RecyclerView.Adapter<ConversationAdapter.MyViewHolder>() {
    override fun getItemCount(): Int {
        return list.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view=LayoutInflater.from(context).inflate(R.layout.conversation_item,parent,false)
        val viewHolder=MyViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val icon=holder.itemView.findViewById(R.id.iconConversation) as ImageView
        val txtConversation=holder.itemView.findViewById(R.id.txtConversation) as TextView
        var speech=""
        if (list.get(position).speaker==0){
            icon.setImageResource(R.drawable.ic_android)
            speech+="<b>Android: </b>"
        }else{
            icon.setImageResource(R.drawable.me)
            speech+="<b>Me: </b>"
        }
        speech+=list.get(position).speech
        txtConversation.text=Html.fromHtml(speech)
    }
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }
}


