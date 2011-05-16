/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.portal.mop.navigation;

import org.exoplatform.commons.utils.Queues;
import org.exoplatform.portal.tree.diff.Adapters;
import org.exoplatform.portal.tree.diff.HierarchyAdapter;
import org.exoplatform.portal.tree.diff.HierarchyChangeIterator;
import org.exoplatform.portal.tree.diff.HierarchyChangeType;
import org.exoplatform.portal.tree.diff.HierarchyDiff;

import java.util.Queue;

/**
 * The update operation.
 *
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 */
class Update
{

   /**
    * Adapter for the update operation.
    *
    * @param <N> the node generic type
    */
   interface Adapter<N>
   {

      /**
       * Returns the data associated with the node or null if such data does not exist.
       *
       * @param node the node
       * @return the node data
       */
      NodeData getData(N node);

      /**
       * The trivial adapter for {@link NodeData} objects.
       */
      Adapter<NodeData> NODE_DATA = new Adapter<NodeData>()
      {
         public NodeData getData(NodeData node)
         {
            return node;
         }
      };

   }

   static <N1, N2> void perform(
      NodeContext<N1> src,
      HierarchyAdapter<String[], NodeContext<N1>, String> srcAdatper,
      N2 dst,
      HierarchyAdapter<String[], N2, String> dstAdapter,
      Adapter<N2> updateAdapter,
      NodeChangeListener<NodeContext<N1>> listener,
      Scope.Visitor visitor)
   {
      // We create the diff object
      HierarchyDiff<String[], NodeContext<N1>, String[], N2, String> diff = HierarchyDiff.create(
         Adapters.<String>list(),
         srcAdatper,
         Adapters.<String>list(),
         dstAdapter,
         Utils.<String>comparator());

      // We obtain the iterator
      HierarchyChangeIterator<String[], NodeContext<N1>, String[], N2, String> it = diff.iterator(src, dst);

      // The last browsed context
      NodeContext<N1> lastCtx = null;

      // This is a not great hack but for now it's ok
      Queue<NodeContext<N1>> addQ = Queues.lifo();

      //
      int depth = 0;

      //
      while (it.hasNext())
      {
         HierarchyChangeType change = it.next();
         switch (change)
         {
            case KEEP:
               break;
            case ENTER:
            {
               // Compute visit
               VisitMode visit = null;
               N2 d = it.getDestination();
               if (d != null)
               {
                  NodeData data = updateAdapter.getData(d);
                  if (data != null)
                  {
                     visit = visitor.enter(depth, data.id, data.name, data.state);
                  }
               }

               // Cut the tree if necessary
               if (visit != VisitMode.ALL_CHILDREN)
               {
                  it.skip();
               }
               else
               {
                  NodeContext<N1> aaa = it.getSource();
                  if (aaa != null)
                  {
                     if (!aaa.isExpanded())
                     {
                        aaa.expand();
                     }
                  }
                  else
                  {
                     addQ.peek().expand();
                  }
               }

               //
               depth++;
               break;
            }
            case LEAVE:
            {
               // Update last context
               lastCtx = it.peekSourceRoot();

               //
               N2 leftDst = it.getDestination();
               if (lastCtx != null && leftDst != null)
               {
                  // Generate node change event (that will occur below)
                  NodeData leftDstData = updateAdapter.getData(leftDst);

                  // Data can be null for transient nodes
                  if (leftDstData != null)
                  {
                     if (!lastCtx.data.state.equals(leftDstData.state))
                     {
                        if (listener != null)
                        {
                           listener.onUpdate(lastCtx,  leftDstData.state);
                        }
                     }

                     // Update name and generate event
                     if (!lastCtx.data.name.equals(leftDstData.name))
                     {
                        lastCtx.name = leftDstData.name;
                        if (listener != null)
                        {
                           listener.onRename(lastCtx, lastCtx.getParent(), leftDstData.name);
                        }
                     }

                     //
                     lastCtx.data = leftDstData;
                  }
               }

               //
               N2 d = it.getDestination();
               if (d != null)
               {
                  NodeData data = updateAdapter.getData(d);
                  if (data != null)
                  {
                     visitor.leave(depth, data.id, data.name, data.state);
                  }
               }

               //
               if (addQ.size() > 0)
               {
                  addQ.poll();
               }

               //
               depth--;
               break;
            }
            case MOVED_OUT:
               break;
            case MOVED_IN:
            {
               NodeContext<N1> to = it.peekSourceRoot();
               NodeContext<N1> moved = it.getSource();
               NodeContext<N1> from = moved.getParent();
               NodeContext<N1> previous;
               if (lastCtx == null || lastCtx.getParent() != to)
               {
                  previous = null;
                  to.insertAt(0, moved);
               }
               else
               {
                  previous = lastCtx;
                  lastCtx.insertAfter(moved);
               }

               //
               if (listener != null)
               {
                  listener.onMove(
                     moved,
                     from,
                     to,
                     previous != null ? previous : null);
               }

               //
               break;
            }
            case ADDED:
            {
               NodeContext<N1> parentCtx = it.peekSourceRoot();
               if (parentCtx == null)
               {
                  parentCtx = addQ.peek();
               }

               //
               NodeContext<N1> addedCtx;
               NodeContext<N1> previousCtx;
               N2 added = it.getDestination();
               NodeData addedData = updateAdapter.getData(added);
               if (lastCtx == null || lastCtx.getParent() != parentCtx)
               {
                  previousCtx = null;
                  addedCtx = parentCtx.insertAt(0, addedData);
               }
               else
               {
                  previousCtx = lastCtx;
                  addedCtx = lastCtx.insertAfter(addedData);
               }

               //
               if (listener != null)
               {
                  listener.onAdd(
                     parentCtx,
                     previousCtx,
                     addedCtx);
               }

               //
               addQ.add(addedCtx);

               //
               break;
            }
            case REMOVED:
            {
               NodeContext<N1> removedCtx = it.getSource();
               NodeContext<N1> parentCtx = removedCtx.getParent();

               //
               removedCtx.remove();

               //
               if (listener != null)
               {
                  listener.onRemove(
                     removedCtx,
                     parentCtx);
               }

               //
               break;
            }
            default:
               throw new UnsupportedOperationException("todo : " + change);
         }
      }
   }
}
