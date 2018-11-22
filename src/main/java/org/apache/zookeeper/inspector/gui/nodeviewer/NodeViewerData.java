/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zookeeper.inspector.gui.nodeviewer;

import org.apache.zookeeper.ZooKeeper.States;
import org.apache.zookeeper.inspector.gui.NodeDataViewerFindDialog;
import org.apache.zookeeper.inspector.gui.ZooInspectorIconResources;
import org.apache.zookeeper.inspector.logger.LoggerFactory;
import org.apache.zookeeper.inspector.manager.ZooInspectorNodeManager;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A node viewer for displaying the data for the currently selected node
 */
public class NodeViewerData extends ZooInspectorNodeViewer {
	private ZooInspectorNodeManager zooInspectorManager;
	private final JTextPane dataArea;
	private final DefaultHighlighter highlighter;
	private String selectedNode;
	
	public void highlight(String selText) {
		highlighter.removeAllHighlights();
		if (selText == null || selText.isEmpty()) {
			return;
		}
		
		selText = selText.toLowerCase();
		DefaultHighlightPainter hPainter = new DefaultHighlightPainter(Color.YELLOW);
		
		// String selText = txtPane.getSelectedText();
		String contText = "";// = jTextPane1.getText();
		
		DefaultStyledDocument document = (DefaultStyledDocument) dataArea.getDocument();
		
		try {
			contText = document.getText(0, document.getLength());
			contText = contText.toLowerCase();
		} catch (BadLocationException ex) {
			LoggerFactory.getLogger().error(null, ex);
		}
		
		int index = -1;
		int firstPos = 0;
		while ((index = contText.indexOf(selText, index)) > -1) {
			if (firstPos == 0) {
				firstPos = index;
			}
			try {
				highlighter.addHighlight(index, selText.length() + index, hPainter);
				index = index + selText.length();
			} catch (BadLocationException ex) {
				LoggerFactory.getLogger().error(null, ex);
				// System.out.println(index);
			}
		}
		
		try {
			// Get the rectangle of the where the text would be visible...
			Rectangle viewRect = dataArea.modelToView(firstPos);
			// Scroll to make the rectangle visible
			dataArea.scrollRectToVisible(viewRect);
			// Highlight the text
			dataArea.setCaretPosition(firstPos);
			// dataArea.moveCaretPosition(index);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
		}
	}
	
	/**
	 *
	 */
	public NodeViewerData() {
		this.setLayout(new BorderLayout());
		this.dataArea = new JTextPane();
		this.highlighter = (DefaultHighlighter) dataArea.getHighlighter();
		
		// add highlighter
		// dataArea.addCaretListener(new CaretListener() {
		//
		// public void caretUpdate(CaretEvent evt) {
		// JTextPane txtPane = (JTextPane) evt.getSource();
		// DefaultHighlighter highlighter = (DefaultHighlighter) txtPane.getHighlighter();
		// if (evt.getDot() == evt.getMark()) {
		// highlighter.removeAllHighlights();
		// return;
		// }
		//
		// highlight(txtPane.getSelectedText());
		// }
		// });
		
		// add search capability
		dataArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if ((e.getKeyCode() == KeyEvent.VK_F) && ((e.getModifiers() & (KeyEvent.CTRL_MASK | KeyEvent.META_MASK)) != 0)) {
					NodeDataViewerFindDialog dialog = new NodeDataViewerFindDialog(NodeViewerData.this);
					dialog.setVisible(true);
				} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
					highlighter.removeAllHighlights();
				}
			}
		});
		dataArea.setEditable(false);
		
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		JScrollPane scroller = new JScrollPane(this.dataArea);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		this.add(scroller, BorderLayout.CENTER);
		this.add(toolbar, BorderLayout.NORTH);
		final JButton saveButton = new JButton(ZooInspectorIconResources.getSaveIcon());
		saveButton.addActionListener(e -> {
			if (selectedNode != null) {
				if (JOptionPane.showConfirmDialog(NodeViewerData.this,
						"Are you sure you want to save this node?" + " (this action cannot be reverted)",
						"Confirm Save", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
					zooInspectorManager.setData(selectedNode, dataArea.getText());
				}
			}
		});
		saveButton.setEnabled(false);
		toolbar.add(saveButton);
		
		// add an edit icon
		JButton editButton = new JButton(ZooInspectorIconResources.getEditIcon());
		editButton.addActionListener(e -> {
			// toggle save button
			if (zooInspectorManager != null && zooInspectorManager.getZookeeperStates() == States.CONNECTED) {
				saveButton.setEnabled(!saveButton.isEnabled());
				dataArea.setEditable(saveButton.isEnabled());
			}
		});
		toolbar.add(editButton);
		
		// add a search icon
		JButton searchButton = new JButton(ZooInspectorIconResources.getSearchIcon());
		String osName = System.getProperty("os.name").toLowerCase();
		String tipText = "Find (^F)";
		if (osName.contains("mac")) {
			tipText = "Find (^/⌘F)";
		}
		searchButton.setToolTipText(tipText);
		searchButton.addActionListener(e -> {
			// if (zooInspectorManager != null && zooInspectorManager.getZookeeperStates() == States.CONNECTED) {
			NodeDataViewerFindDialog dialog = new NodeDataViewerFindDialog(NodeViewerData.this);
			dialog.setVisible(true);
			// }
		});
		toolbar.add(searchButton);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see
	 * org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer#
	 * getTitle()
	 */
	@Override
	public String getTitle() {
		return "Node Data";
	}
	
	/*
	 * (non-Javadoc)
	 * @see
	 * org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer#
	 * nodeSelectionChanged(java.util.Set)
	 */
	@Override
	public void nodeSelectionChanged(List<String> selectedNodes) {
		if (selectedNodes.size() > 0) {
//      final long start = System.currentTimeMillis();
			
			this.selectedNode = selectedNodes.get(0);
			SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
				
				@Override
				protected String doInBackground() {
					return NodeViewerData.this.zooInspectorManager.getData(NodeViewerData.this.selectedNode);
				}
				
				@Override
				protected void done() {
					String data = "";
					try {
						data = get();
					} catch (InterruptedException | ExecutionException e) {
						LoggerFactory.getLogger().error(
								"Error retrieving data for node: " + NodeViewerData.this.selectedNode, e);
					}
					NodeViewerData.this.dataArea.setText(data);
					NodeViewerData.this.dataArea.setCaretPosition(0);
					// NodeViewerData.this.dataArea.moveCaretPosition(0);
//          long end = System.currentTimeMillis();
//          System.out.println("NodeViewerData.nodeSelectionChanged() invoked. took: "
//              + (end - start));
				
				}
			};
			worker.execute();
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see
	 * org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer#
	 * setZooInspectorManager
	 * (org.apache.zookeeper.inspector.manager.ZooInspectorNodeManager)
	 */
	@Override
	public void setZooInspectorManager(ZooInspectorNodeManager zooInspectorManager) {
		this.zooInspectorManager = zooInspectorManager;
	}
	
}
