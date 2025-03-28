import { expect, test } from '@playwright/test';

test('canLoginAndLogout', async ({ page }) => {
  await page.goto('http://localhost:3000/login');
  await page.getByRole('textbox', { name: 'Benutzername' }).click();
  await page.getByRole('textbox', { name: 'Benutzername' }).fill('c.triebstein@arcor.de');
  await page.getByRole('textbox', { name: 'Benutzername' }).press('Tab');
  await page.getByRole('textbox', { name: 'Passwort' }).fill('password');
  await page.getByRole('button', { name: 'Anmelden' }).click();

  await expect(page.getByRole('button', { name: 'c.triebstein@arcor.de' })).toBeVisible();
  await expect(page.getByRole('button').filter({ hasText: /^$/ }).first()).toBeVisible();
  await expect(page.getByRole('button', { name: 'Teilnehmer verwalten' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Logout' })).toBeVisible();
  await page.getByRole('button', { name: 'Logout' }).click();

  await expect(page.getByRole('button', { name: 'Login' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Anmelden' })).toBeVisible();
});