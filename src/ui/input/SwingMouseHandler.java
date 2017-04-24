package ui.input;

import language.splitter.FoundWord;
import ui.UI;

import java.awt.*;
import java.awt.event.*;

import static main.Main.currPage;
import static main.Main.options;
import static ui.UI.*;
import static ui.UI.mainFontSize;

/**
 * Created by laure on 2017/04/24.
 */
public class SwingMouseHandler extends MouseHandler implements MouseListener, MouseMotionListener, MouseWheelListener
{
    private boolean lMouseClick = false;
    private boolean lMouseState = false;
    private Point dragReference;
    private long lastClickTime = 0;

    private static final long MAX_CLICK_DELAY = 1000;
    private static final long MIN_DRAG_DIST = 100;

    public SwingMouseHandler(UI ui)
    {
        super(ui);
    }

    @Override
    public void addListeners()
    {
        ui.disp.getFrame().addMouseListener(this);
        ui.disp.getFrame().addMouseMotionListener(this);
        ui.disp.getFrame().addMouseWheelListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        long clickTime = System.nanoTime();
        if(clickTime - lastClickTime < MAX_CLICK_DELAY)
        {
            System.out.println("stop double event");
            return;//[attempt to]stop accidental double click
        }
        lastClickTime = clickTime;

        if(e.getButton() == 1)lMouseState = false;

        if(e.getButton() == 1 && lMouseClick)//if left click (and wasn't drag)
        {
            leftClick(e.getPoint());
            lMouseClick = false;
        }
        else if(e.getButton() == 2)//middle click: place marker
        {
            middleClick(e.getPoint());
        }
        else if(e.getButton() == 3)//right click: extra option menu
        {
            rightClick(e.getPoint());
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        dragReference = e.getPoint();
        if(e.getButton() == 1)
        {
            lMouseClick = true;
            if(e.getY() >= furiganaStartY && e.getY() <= textStartY)//only furigana bar draggable
            {
                lMouseState = true;
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {

        double dist = dragReference.distanceSq(e.getPoint());
        if((dist != 0 || lMouseState) && dist < MIN_DRAG_DIST)//only moved a little
        {
            if(e.getButton() == 1)lMouseClick = true;
            lMouseState = false;
            mouseClicked(e);//pass this over as a click
        }
        else if (!lMouseState)//long drag, not on Furigana bar
        {
            //place splits at start and end points of drag
            //TODO place split points
            //TODO show first split point when starting the drag
            //TODO select word on release
        }
        lMouseState = false;
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        //temporary ignore loose focus
        if(tempIgnoreMouseExit)
        {
            return;
        }
        //collapse definitions
        if(ui.selectedWord != null && options.getOptionBool("hideDefOnMouseLeave"))
        {
            ui.selectedWord.showDef(false);
            ui.selectedWord = null;
            ui.render();
        }
        if(mousedWord != null)
        {
            mousedWord.setMouseover(false);
            boolean rerender = mousedWord.updateOnMouse();
            mousedWord = null;
            if(rerender)ui.render();
        }
        mouseLine = -1;
        mousePos = null;
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if(lMouseState)
        {
            Point moveTo = e.getLocationOnScreen();
            moveTo.translate(-dragReference.x, -dragReference.y);
            ui.disp.getFrame().setLocation(moveTo);
            lMouseClick = false;//no longer a click
        }
    }


    private int mouseLine = -1;
    private FoundWord mousedWord;
    //TODO move this to MouseHandler
    @Override
    public void mouseMoved(MouseEvent e)
    {
        mousePos = e.getPoint();
        int pos = toCharPos(e.getX());
        int lineIndex = ui.getLineIndex(e.getPoint());
        if(lineIndex >= currPage.getLineCount() || lineIndex < 0)return;
        if(lineIndex != mouseLine || (mousedWord!= null && !mousedWord.inBounds(pos)))
        {
            boolean reRender = false;
            if(mousedWord != null)
            {
                mousedWord.setMouseover(false);
                if(mousedWord.updateOnMouse())reRender = true;
            }
            mousedWord = null;//to recalculate
            //toggle on selected line:
            for (FoundWord word : currPage.getLine(lineIndex).getWords())
            {
                if (word.inBounds(pos))
                {
                    mousedWord = word;
                    break;
                }
            }
            mouseLine = lineIndex;

            if(mousedWord != null)
            {
                //System.out.println("mouseover'd word changed to " + mousedWord.getText());
                mousedWord.setMouseover(true);
                if(mousedWord.updateOnMouse())reRender = true;
            }

            if(reRender)ui.render();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        if(hidden)return;
        mouseScroll(e.getWheelRotation(), e.getPoint());
    }
}
