import type { Locator, Page } from '@playwright/test'

const MIN_READING_MS = 2_500
const READING_MS_PER_WORD = 300
const CURSOR_GLIDE_MS = 700
const TYPING_DELAY_MS = 70

interface Rect {
  x: number
  y: number
  width: number
  height: number
}

interface DemoOverlay {
  show(text: string, rect: Rect | null): void
  hide(): void
  cursor(x: number, y: number): void
}

declare global {
  interface Window {
    __demo?: DemoOverlay
  }
}

// Runs in the browser on every page load (serialized by addInitScript, so it must be self-contained).
// Draws a speech balloon, an outline around the element in use and a cursor dot. None of it takes clicks.
function installOverlay() {
  const GAP = 14
  const MARGIN = 16
  const BALLOON_MAX_WIDTH = 380
  const SIDE_MIN_WIDTH = 240
  const css = `
    #demo-balloon { position: fixed; z-index: 2147483647; max-width: 380px; padding: 12px 16px; border-radius: 12px;
      background: #0f172a; color: #f8fafc; font: 500 16px/1.45 system-ui, sans-serif; box-shadow: 0 10px 30px rgb(15 23 42 / .35);
      pointer-events: none; opacity: 0; transition: opacity .25s, top .25s, left .25s; }
    #demo-balloon.title { max-width: 560px; padding: 20px 26px; font-size: 22px; text-align: center; }
    #demo-highlight { position: fixed; z-index: 2147483646; border: 3px solid #0ea5e9; border-radius: 10px;
      background: rgb(14 165 233 / .08); pointer-events: none; opacity: 0; transition: all .25s; }
    #demo-cursor { position: fixed; z-index: 2147483647; width: 20px; height: 20px; margin: -10px 0 0 -10px; border-radius: 50%;
      background: rgb(244 63 94 / .85); border: 2px solid #fff; box-shadow: 0 2px 8px rgb(0 0 0 / .4); pointer-events: none;
      transition: left .7s ease-in-out, top .7s ease-in-out; left: 50%; top: 50%; }`

  function element(id: string) {
    let node = document.getElementById(id)
    if (!node) {
      if (!document.getElementById('demo-style')) {
        const style = document.createElement('style')
        style.id = 'demo-style'
        style.textContent = css
        document.head.appendChild(style)
      }
      node = document.createElement('div')
      node.id = id
      document.body.appendChild(node)
    }
    return node
  }

  window.__demo = {
    show(text, rect) {
      const balloon = element('demo-balloon')
      const highlight = element('demo-highlight')
      balloon.textContent = text
      balloon.className = rect ? '' : 'title'
      balloon.style.opacity = '1'
      balloon.style.maxWidth = ''
      if (!rect) {
        const { width, height } = balloon.getBoundingClientRect()
        highlight.style.opacity = '0'
        balloon.style.left = `${(innerWidth - width) / 2}px`
        balloon.style.top = `${(innerHeight - height) / 2}px`
        return
      }
      Object.assign(highlight.style, {
        left: `${rect.x - 6}px`,
        top: `${rect.y - 6}px`,
        width: `${rect.width + 12}px`,
        height: `${rect.height + 12}px`,
        opacity: '1',
      })
      // Beside the target when there is room, so the balloon never covers the next control; otherwise below or above.
      const besideLeft = rect.x + rect.width + GAP
      const besideWidth = innerWidth - besideLeft - MARGIN
      if (besideWidth >= SIDE_MIN_WIDTH) {
        balloon.style.maxWidth = `${Math.min(BALLOON_MAX_WIDTH, besideWidth)}px`
        const { height } = balloon.getBoundingClientRect()
        const centred = rect.y + rect.height / 2 - height / 2
        balloon.style.left = `${besideLeft}px`
        balloon.style.top = `${Math.min(Math.max(MARGIN, centred), innerHeight - height - MARGIN)}px`
        return
      }
      const { width, height } = balloon.getBoundingClientRect()
      const below = rect.y + rect.height + GAP
      const top = below + height + MARGIN < innerHeight ? below : Math.max(MARGIN, rect.y - height - GAP)
      const left = Math.min(Math.max(MARGIN, rect.x), innerWidth - width - MARGIN)
      balloon.style.top = `${top}px`
      balloon.style.left = `${left}px`
    },
    hide() {
      element('demo-balloon').style.opacity = '0'
      element('demo-highlight').style.opacity = '0'
    },
    cursor(x, y) {
      const cursor = element('demo-cursor')
      cursor.style.left = `${x}px`
      cursor.style.top = `${y}px`
    },
  }
}

function readingTime(text: string) {
  return Math.max(MIN_READING_MS, text.split(/\s+/).length * READING_MS_PER_WORD)
}

// Narrates a demo chapter: balloons explain each step next to the element in use.
export class Narrator {
  private constructor(private readonly page: Page) {}

  static async attach(page: Page) {
    await page.addInitScript(installOverlay)
    return new Narrator(page)
  }

  // A balloon next to the target, or a centred title card when there is no target.
  async say(text: string, target?: Locator) {
    let rect: Rect | null = null
    if (target) {
      await target.scrollIntoViewIfNeeded()
      rect = await target.boundingBox()
    }
    await this.page.evaluate(([message, box]) => window.__demo?.show(message, box), [text, rect] as const)
    await this.page.waitForTimeout(readingTime(text))
  }

  async hide() {
    await this.page.evaluate(() => window.__demo?.hide())
  }

  async click(target: Locator, text?: string) {
    if (text) {
      await this.say(text, target)
    }
    await this.pointAt(target)
    await target.click()
    await this.hide()
  }

  async type(target: Locator, value: string, text?: string) {
    if (text) {
      await this.say(text, target)
    }
    await this.pointAt(target)
    await target.fill('')
    await target.pressSequentially(value, { delay: TYPING_DELAY_MS })
  }

  private async pointAt(target: Locator) {
    await target.scrollIntoViewIfNeeded()
    const box = await target.boundingBox()
    if (box) {
      await this.page.evaluate(([x, y]) => window.__demo?.cursor(x, y), [box.x + box.width / 2, box.y + box.height / 2] as const)
      await this.page.waitForTimeout(CURSOR_GLIDE_MS)
    }
  }
}
